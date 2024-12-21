package module;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;

/*
 * Copyright © 2024 Project Nayuki. (MIT License)
 * https://www.nayuki.io/page/qr-code-generator-library
 */

public final class QrCode {

	private String data = "";
	private int color = 0x000000;
	private String imageName = "qr_code_fixed";
	private String imageFormat = "png";

	private List<QrModule> moduleList =  new ArrayList<>();

	public void setData (String data) {
		this.data = data;
	}
	public String getData () {
		return data;
	}
	public void setColor(int color) {
		this.color = color;
	}
	public void setImage(String imageName, String imageFormat) {
		this.imageName = imageName;
		this.imageFormat = imageFormat;
	}
	public QrCode addModule(QrModule module) {
		moduleList.add(module);
		return this;
	}

	public void initialize() throws IOException {
		for (QrModule module : moduleList) {
			module.applyModule(this);
		}
		QrCode qr = encodeText(this.data, Ecc.LOW);
		BufferedImage image = toImage(qr, 10, 1,  this.color);
		String pathName = imageName+"."+imageFormat;
		File file = new File(pathName);
		ImageIO.write(image, "png", file);
	}

	public QrCode(String data) {
		this.data = data;
	}

	private BufferedImage toImage(QrCode qr, int scale, int border, int qrColor) {
		Objects.requireNonNull(qr);
		if (scale <= 0 || border < 0)
			throw new IllegalArgumentException("Value out of range");
		if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
			throw new IllegalArgumentException("Scale or border too large");

		BufferedImage result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < result.getHeight(); y++) {
			for (int x = 0; x < result.getWidth(); x++) {
				boolean color = qr.getModule(x / scale - border, y / scale - border);
				result.setRGB(x, y, color ? qrColor : 0xFFFFFF);
			}
		}
		return result;
	}
	///////
	public static QrCode encodeText(CharSequence text, Ecc ecl) {
		Objects.requireNonNull(text);
		Objects.requireNonNull(ecl);
		List<QrSegment> segs = QrSegment.makeSegments(text);
		return encodeSegments(segs, ecl);
	}
	
	public static QrCode encodeBinary(byte[] data, Ecc ecl) {
		Objects.requireNonNull(data);
		Objects.requireNonNull(ecl);
		QrSegment seg = QrSegment.makeBytes(data);
		return encodeSegments(Arrays.asList(seg), ecl);
	}
	
	
	public static QrCode encodeSegments(List<QrSegment> segs, Ecc ecl) {
		return encodeSegments(segs, ecl, MIN_VERSION, MAX_VERSION, -1, true);
	}
	
	
	public static QrCode encodeSegments(List<QrSegment> segs, Ecc ecl, int minVersion, int maxVersion, int mask, boolean boostEcl) {
		Objects.requireNonNull(segs);
		Objects.requireNonNull(ecl);
		if (!(MIN_VERSION <= minVersion && minVersion <= maxVersion && maxVersion <= MAX_VERSION) || mask < -1 || mask > 7)
			throw new IllegalArgumentException("Invalid value");
		
		int version, dataUsedBits;
		for (version = minVersion; ; version++) {
			int dataCapacityBits = getNumDataCodewords(version, ecl) * 8;
			dataUsedBits = QrSegment.getTotalBits(segs, version);
			if (dataUsedBits != -1 && dataUsedBits <= dataCapacityBits)
				break;
			if (version >= maxVersion) {
				String msg = "Segment too long";
				if (dataUsedBits != -1)
					msg = String.format("Data length = %d bits, Max capacity = %d bits", dataUsedBits, dataCapacityBits);
			}
		}
		assert dataUsedBits != -1;
		
		for (Ecc newEcl : Ecc.values()) {
			if (boostEcl && dataUsedBits <= getNumDataCodewords(version, newEcl) * 8)
				ecl = newEcl;
		}
		
		BitBuffer bb = new BitBuffer();
		for (QrSegment seg : segs) {
			bb.appendBits(seg.mode.modeBits, 4);
			bb.appendBits(seg.numChars, seg.mode.numCharCountBits(version));
			bb.appendData(seg.data);
		}
		assert bb.bitLength() == dataUsedBits;
		
		int dataCapacityBits = getNumDataCodewords(version, ecl) * 8;
		assert bb.bitLength() <= dataCapacityBits;
		bb.appendBits(0, Math.min(4, dataCapacityBits - bb.bitLength()));
		bb.appendBits(0, (8 - bb.bitLength() % 8) % 8);
		assert bb.bitLength() % 8 == 0;
		
		for (int padByte = 0xEC; bb.bitLength() < dataCapacityBits; padByte ^= 0xEC ^ 0x11)
			bb.appendBits(padByte, 8);
		
		byte[] dataCodewords = new byte[bb.bitLength() / 8];
		for (int i = 0; i < bb.bitLength(); i++)
			dataCodewords[i >>> 3] |= bb.getBit(i) << (7 - (i & 7));
		
		return new QrCode(version, ecl, dataCodewords, mask);
	}
	
	
	
	public int version;

	public int size;
	
	public Ecc errorCorrectionLevel;
	
	public int mask;
	
	private boolean[][] modules;
	
	private boolean[][] isFunction;
	
	public QrCode(int ver, Ecc ecl, byte[] dataCodewords, int msk) {
		if (ver < MIN_VERSION || ver > MAX_VERSION)
			throw new IllegalArgumentException("Version value out of range");
		if (msk < -1 || msk > 7)
			throw new IllegalArgumentException("Mask value out of range");
		version = ver;
		size = ver * 4 + 17;
		errorCorrectionLevel = Objects.requireNonNull(ecl);
		Objects.requireNonNull(dataCodewords);
		modules    = new boolean[size][size];
		isFunction = new boolean[size][size];
		
		drawFunctionPatterns();
		byte[] allCodewords = addEccAndInterleave(dataCodewords);
		drawCodewords(allCodewords);
		
		if (msk == -1) {
			int minPenalty = Integer.MAX_VALUE;
			for (int i = 0; i < 8; i++) {
				applyMask(i);
				drawFormatBits(i);
				int penalty = getPenaltyScore();
				if (penalty < minPenalty) {
					msk = i;
					minPenalty = penalty;
				}
				applyMask(i);
			}
		}
		assert 0 <= msk && msk <= 7;
		mask = msk;
		applyMask(msk);
		drawFormatBits(msk);
		
		isFunction = null;
	}
	public boolean getModule(int x, int y) {
		return 0 <= x && x < size && 0 <= y && y < size && modules[y][x];
	}
	private void drawFunctionPatterns() {
		for (int i = 0; i < size; i++) {
			setFunctionModule(6, i, i % 2 == 0);
			setFunctionModule(i, 6, i % 2 == 0);
		}

		drawFinderPattern(3, 3);
		drawFinderPattern(size - 4, 3);
		drawFinderPattern(3, size - 4);
		
		int[] alignPatPos = getAlignmentPatternPositions();
		int numAlign = alignPatPos.length;
		for (int i = 0; i < numAlign; i++) {
			for (int j = 0; j < numAlign; j++) {
				if (!(i == 0 && j == 0 || i == 0 && j == numAlign - 1 || i == numAlign - 1 && j == 0))
					drawAlignmentPattern(alignPatPos[i], alignPatPos[j]);
			}
		}
		
		drawFormatBits(0);
		drawVersion();
	}
	
	private void drawFormatBits(int msk) {
		int data = errorCorrectionLevel.formatBits << 3 | msk;
		int rem = data;
		for (int i = 0; i < 10; i++)
			rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
		int bits = (data << 10 | rem) ^ 0x5412;
		assert bits >>> 15 == 0;
		
		for (int i = 0; i <= 5; i++)
			setFunctionModule(8, i, getBit(bits, i));
		setFunctionModule(8, 7, getBit(bits, 6));
		setFunctionModule(8, 8, getBit(bits, 7));
		setFunctionModule(7, 8, getBit(bits, 8));
		for (int i = 9; i < 15; i++)
			setFunctionModule(14 - i, 8, getBit(bits, i));
		
		for (int i = 0; i < 8; i++)
			setFunctionModule(size - 1 - i, 8, getBit(bits, i));
		for (int i = 8; i < 15; i++)
			setFunctionModule(8, size - 15 + i, getBit(bits, i));
		setFunctionModule(8, size - 8, true);  // Always dark
	}
	private void drawVersion() {
		if (version < 7)
			return;
		
		int rem = version;
		for (int i = 0; i < 12; i++)
			rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
		int bits = version << 12 | rem;  // uint18
		assert bits >>> 18 == 0;
		
		for (int i = 0; i < 18; i++) {
			boolean bit = getBit(bits, i);
			int a = size - 11 + i % 3;
			int b = i / 3;
			setFunctionModule(a, b, bit);
			setFunctionModule(b, a, bit);
		}
	}
	
	private void drawFinderPattern(int x, int y) {
		for (int dy = -4; dy <= 4; dy++) {
			for (int dx = -4; dx <= 4; dx++) {
				int dist = Math.max(Math.abs(dx), Math.abs(dy));  // Chebyshev/infinity norm
				int xx = x + dx, yy = y + dy;
				if (0 <= xx && xx < size && 0 <= yy && yy < size)
					setFunctionModule(xx, yy, dist != 2 && dist != 4);
			}
		}
	}
	
	private void drawAlignmentPattern(int x, int y) {
		for (int dy = -2; dy <= 2; dy++) {
			for (int dx = -2; dx <= 2; dx++)
				setFunctionModule(x + dx, y + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
		}
	}
	
	
	private void setFunctionModule(int x, int y, boolean isDark) {
		modules[y][x] = isDark;
		isFunction[y][x] = true;
	}
	
	
	private byte[] addEccAndInterleave(byte[] data) {
		Objects.requireNonNull(data);
		if (data.length != getNumDataCodewords(version, errorCorrectionLevel))
			throw new IllegalArgumentException();
		
		// Calculate parameter numbers
		int numBlocks = NUM_ERROR_CORRECTION_BLOCKS[errorCorrectionLevel.ordinal()][version];
		int blockEccLen = ECC_CODEWORDS_PER_BLOCK  [errorCorrectionLevel.ordinal()][version];
		int rawCodewords = getNumRawDataModules(version) / 8;
		int numShortBlocks = numBlocks - rawCodewords % numBlocks;
		int shortBlockLen = rawCodewords / numBlocks;
		
		byte[][] blocks = new byte[numBlocks][];
		byte[] rsDiv = reedSolomonComputeDivisor(blockEccLen);
		for (int i = 0, k = 0; i < numBlocks; i++) {
			byte[] dat = Arrays.copyOfRange(data, k, k + shortBlockLen - blockEccLen + (i < numShortBlocks ? 0 : 1));
			k += dat.length;
			byte[] block = Arrays.copyOf(dat, shortBlockLen + 1);
			byte[] ecc = reedSolomonComputeRemainder(dat, rsDiv);
			System.arraycopy(ecc, 0, block, block.length - blockEccLen, ecc.length);
			blocks[i] = block;
		}
		byte[] result = new byte[rawCodewords];
		for (int i = 0, k = 0; i < blocks[0].length; i++) {
			for (int j = 0; j < blocks.length; j++) {
				// Skip the padding byte in short blocks
				if (i != shortBlockLen - blockEccLen || j >= numShortBlocks) {
					result[k] = blocks[j][i];
					k++;
				}
			}
		}
		return result;
	}
	
	
	private void drawCodewords(byte[] data) {
		Objects.requireNonNull(data);
		if (data.length != getNumRawDataModules(version) / 8)
			throw new IllegalArgumentException();
		
		int i = 0;
		for (int right = size - 1; right >= 1; right -= 2) {
			if (right == 6)
				right = 5;
			for (int vert = 0; vert < size; vert++) {
				for (int j = 0; j < 2; j++) {
					int x = right - j;
					boolean upward = ((right + 1) & 2) == 0;
					int y = upward ? size - 1 - vert : vert;
					if (!isFunction[y][x] && i < data.length * 8) {
						modules[y][x] = getBit(data[i >>> 3], 7 - (i & 7));
						i++;
					}
				}
			}
		}
		assert i == data.length * 8;
	}
	
	private void applyMask(int msk) {
		if (msk < 0 || msk > 7)
			throw new IllegalArgumentException("Mask value out of range");
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				boolean invert;
				switch (msk) {
					case 0:  invert = (x + y) % 2 == 0;                    break;
					case 1:  invert = y % 2 == 0;                          break;
					case 2:  invert = x % 3 == 0;                          break;
					case 3:  invert = (x + y) % 3 == 0;                    break;
					case 4:  invert = (x / 3 + y / 2) % 2 == 0;            break;
					case 5:  invert = x * y % 2 + x * y % 3 == 0;          break;
					case 6:  invert = (x * y % 2 + x * y % 3) % 2 == 0;    break;
					case 7:  invert = ((x + y) % 2 + x * y % 3) % 2 == 0;  break;
					default:  throw new AssertionError();
				}
				modules[y][x] ^= invert & !isFunction[y][x];
			}
		}
	}
	
	private int getPenaltyScore() {
		int result = 0;
		
		for (int y = 0; y < size; y++) {
			boolean runColor = false;
			int runX = 0;
			int[] runHistory = new int[7];
			for (int x = 0; x < size; x++) {
				if (modules[y][x] == runColor) {
					runX++;
					if (runX == 5)
						result += PENALTY_N1;
					else if (runX > 5)
						result++;
				} else {
					finderPenaltyAddHistory(runX, runHistory);
					if (!runColor)
						result += finderPenaltyCountPatterns(runHistory) * PENALTY_N3;
					runColor = modules[y][x];
					runX = 1;
				}
			}
			result += finderPenaltyTerminateAndCount(runColor, runX, runHistory) * PENALTY_N3;
		}
		for (int x = 0; x < size; x++) {
			boolean runColor = false;
			int runY = 0;
			int[] runHistory = new int[7];
			for (int y = 0; y < size; y++) {
				if (modules[y][x] == runColor) {
					runY++;
					if (runY == 5)
						result += PENALTY_N1;
					else if (runY > 5)
						result++;
				} else {
					finderPenaltyAddHistory(runY, runHistory);
					if (!runColor)
						result += finderPenaltyCountPatterns(runHistory) * PENALTY_N3;
					runColor = modules[y][x];
					runY = 1;
				}
			}
			result += finderPenaltyTerminateAndCount(runColor, runY, runHistory) * PENALTY_N3;
		}
		for (int y = 0; y < size - 1; y++) {
			for (int x = 0; x < size - 1; x++) {
				boolean color = modules[y][x];
				if (  color == modules[y][x + 1] &&
				      color == modules[y + 1][x] &&
				      color == modules[y + 1][x + 1])
					result += PENALTY_N2;
			}
		}
		
		int dark = 0;
		for (boolean[] row : modules) {
			for (boolean color : row) {
				if (color)
					dark++;
			}
		}
		int total = size * size;
		int k = (Math.abs(dark * 20 - total * 10) + total - 1) / total - 1;
		assert 0 <= k && k <= 9;
		result += k * PENALTY_N4;
		assert 0 <= result && result <= 2568888;
		return result;
	}
	
	private int[] getAlignmentPatternPositions() {
		if (version == 1)
			return new int[]{};
		else {
			int numAlign = version / 7 + 2;
			int step = (version * 8 + numAlign * 3 + 5) / (numAlign * 4 - 4) * 2;
			int[] result = new int[numAlign];
			result[0] = 6;
			for (int i = result.length - 1, pos = size - 7; i >= 1; i--, pos -= step)
				result[i] = pos;
			return result;
		}
	}
	
	private static int getNumRawDataModules(int ver) {
		if (ver < MIN_VERSION || ver > MAX_VERSION)
			throw new IllegalArgumentException("Version number out of range");
		
		int size = ver * 4 + 17;
		int result = size * size;
		result -= 8 * 8 * 3;
		result -= 15 * 2 + 1;
		result -= (size - 16) * 2;
		if (ver >= 2) {
			int numAlign = ver / 7 + 2;
			result -= (numAlign - 1) * (numAlign - 1) * 25;
			result -= (numAlign - 2) * 2 * 20;
			if (ver >= 7)
				result -= 6 * 3 * 2;
		}
		assert 208 <= result && result <= 29648;
		return result;
	}
	
	
	private static byte[] reedSolomonComputeDivisor(int degree) {
		if (degree < 1 || degree > 255)
			throw new IllegalArgumentException("Degree out of range");
		byte[] result = new byte[degree];
		result[degree - 1] = 1;
		
		int root = 1;
		for (int i = 0; i < degree; i++) {
			for (int j = 0; j < result.length; j++) {
				result[j] = (byte)reedSolomonMultiply(result[j] & 0xFF, root);
				if (j + 1 < result.length)
					result[j] ^= result[j + 1];
			}
			root = reedSolomonMultiply(root, 0x02);
		}
		return result;
	}
	
	
	private static byte[] reedSolomonComputeRemainder(byte[] data, byte[] divisor) {
		Objects.requireNonNull(data);
		Objects.requireNonNull(divisor);
		byte[] result = new byte[divisor.length];
		for (byte b : data) {
			int factor = (b ^ result[0]) & 0xFF;
			System.arraycopy(result, 1, result, 0, result.length - 1);
			result[result.length - 1] = 0;
			for (int i = 0; i < result.length; i++)
				result[i] ^= reedSolomonMultiply(divisor[i] & 0xFF, factor);
		}
		return result;
	}
	
	private static int reedSolomonMultiply(int x, int y) {
		assert x >> 8 == 0 && y >> 8 == 0;
		int z = 0;
		for (int i = 7; i >= 0; i--) {
			z = (z << 1) ^ ((z >>> 7) * 0x11D);
			z ^= ((y >>> i) & 1) * x;
		}
		assert z >>> 8 == 0;
		return z;
	}
	
	static int getNumDataCodewords(int ver, Ecc ecl) {
		return getNumRawDataModules(ver) / 8
			- ECC_CODEWORDS_PER_BLOCK    [ecl.ordinal()][ver]
			* NUM_ERROR_CORRECTION_BLOCKS[ecl.ordinal()][ver];
	}
	
	private int finderPenaltyCountPatterns(int[] runHistory) {
		int n = runHistory[1];
		assert n <= size * 3;
		boolean core = n > 0 && runHistory[2] == n && runHistory[3] == n * 3 && runHistory[4] == n && runHistory[5] == n;
		return (core && runHistory[0] >= n * 4 && runHistory[6] >= n ? 1 : 0)
		     + (core && runHistory[6] >= n * 4 && runHistory[0] >= n ? 1 : 0);
	}
	
	private int finderPenaltyTerminateAndCount(boolean currentRunColor, int currentRunLength, int[] runHistory) {
		if (currentRunColor) {
			finderPenaltyAddHistory(currentRunLength, runHistory);
			currentRunLength = 0;
		}
		currentRunLength += size;
		finderPenaltyAddHistory(currentRunLength, runHistory);
		return finderPenaltyCountPatterns(runHistory);
	}
	
	
	private void finderPenaltyAddHistory(int currentRunLength, int[] runHistory) {
		if (runHistory[0] == 0)
			currentRunLength += size;
		System.arraycopy(runHistory, 0, runHistory, 1, runHistory.length - 1);
		runHistory[0] = currentRunLength;
	}
	
	
	static boolean getBit(int x, int i) {
		return ((x >>> i) & 1) != 0;
	}
	
	public static final int MIN_VERSION =  1;
	public static final int MAX_VERSION = 40;
	
	private static final int PENALTY_N1 =  3;
	private static final int PENALTY_N2 =  3;
	private static final int PENALTY_N3 = 40;
	private static final int PENALTY_N4 = 10;
	
	
	private static final byte[][] ECC_CODEWORDS_PER_BLOCK = {
		{-1,  7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},  // Low
		{-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28},  // Medium
		{-1, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},  // Quartile
		{-1, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30},  // High
	};
	
	private static final byte[][] NUM_ERROR_CORRECTION_BLOCKS = {
		{-1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4,  4,  4,  4,  4,  6,  6,  6,  6,  7,  8,  8,  9,  9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25},  // Low
		{-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5,  5,  8,  9,  9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49},  // Medium
		{-1, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8,  8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68},  // Quartile
		{-1, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81},  // High
	};
	
	public enum Ecc {
		LOW(1),
		MEDIUM(0),
		QUARTILE(3),
		HIGH(2);
		
		final int formatBits;
		
		private Ecc(int fb) {
			formatBits = fb;
		}
	}
	
}
