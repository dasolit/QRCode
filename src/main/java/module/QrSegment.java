package module;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;


public final class QrSegment {

	public static QrSegment makeBytes(byte[] data) {
		Objects.requireNonNull(data);
		BitBuffer bb = new BitBuffer();
		for (byte b : data)
			bb.appendBits(b & 0xFF, 8);
		return new QrSegment(Mode.BYTE, data.length, bb);
	}
	public static QrSegment makeNumeric(CharSequence digits) {
		Objects.requireNonNull(digits);
		if (!isNumeric(digits))
			throw new IllegalArgumentException("String contains non-numeric characters");
		
		BitBuffer bb = new BitBuffer();
		for (int i = 0; i < digits.length(); ) {  // Consume up to 3 digits per iteration
			int n = Math.min(digits.length() - i, 3);
			bb.appendBits(Integer.parseInt(digits.subSequence(i, i + n).toString()), n * 3 + 1);
			i += n;
		}
		return new QrSegment(Mode.NUMERIC, digits.length(), bb);
	}
	
	public static QrSegment makeAlphanumeric(CharSequence text) {
		Objects.requireNonNull(text);
		if (!isAlphanumeric(text))
			throw new IllegalArgumentException("String contains unencodable characters in alphanumeric mode");
		
		BitBuffer bb = new BitBuffer();
		int i;
		for (i = 0; i <= text.length() - 2; i += 2) {  // Process groups of 2
			int temp = ALPHANUMERIC_CHARSET.indexOf(text.charAt(i)) * 45;
			temp += ALPHANUMERIC_CHARSET.indexOf(text.charAt(i + 1));
			bb.appendBits(temp, 11);
		}
		if (i < text.length())  // 1 character remaining
			bb.appendBits(ALPHANUMERIC_CHARSET.indexOf(text.charAt(i)), 6);
		return new QrSegment(Mode.ALPHANUMERIC, text.length(), bb);
	}
	
	public static List<QrSegment> makeSegments(CharSequence text) {
		Objects.requireNonNull(text);
		
		List<QrSegment> result = new ArrayList<>();
		if (text.equals(""));  // Leave result empty
		else if (isNumeric(text))
			result.add(makeNumeric(text));
		else if (isAlphanumeric(text))
			result.add(makeAlphanumeric(text));
		else
			result.add(makeBytes(text.toString().getBytes(StandardCharsets.UTF_8)));
		return result;
	}
	
	public static QrSegment makeEci(int assignVal) {
		BitBuffer bb = new BitBuffer();
		if (assignVal < 0)
			throw new IllegalArgumentException("ECI assignment value out of range");
		else if (assignVal < (1 << 7))
			bb.appendBits(assignVal, 8);
		else if (assignVal < (1 << 14)) {
			bb.appendBits(0b10, 2);
			bb.appendBits(assignVal, 14);
		} else if (assignVal < 1_000_000) {
			bb.appendBits(0b110, 3);
			bb.appendBits(assignVal, 21);
		} else
			throw new IllegalArgumentException("ECI assignment value out of range");
		return new QrSegment(Mode.ECI, 0, bb);
	}
	
	public static boolean isNumeric(CharSequence text) {
		return NUMERIC_REGEX.matcher(text).matches();
	}
	
	public static boolean isAlphanumeric(CharSequence text) {
		return ALPHANUMERIC_REGEX.matcher(text).matches();
	}

	public final Mode mode;

	public final int numChars;

	final BitBuffer data;
	
	public QrSegment(Mode md, int numCh, BitBuffer data) {
		mode = Objects.requireNonNull(md);
		Objects.requireNonNull(data);
		if (numCh < 0)
			throw new IllegalArgumentException("Invalid value");
		numChars = numCh;
		this.data = data.clone();  // Make defensive copy
	}

	public BitBuffer getData() {
		return data.clone();  // Make defensive copy
	}
	
	static int getTotalBits(List<QrSegment> segs, int version) {
		Objects.requireNonNull(segs);
		long result = 0;
		for (QrSegment seg : segs) {
			Objects.requireNonNull(seg);
			int ccbits = seg.mode.numCharCountBits(version);
			if (seg.numChars >= (1 << ccbits))
				return -1;  // The segment's length doesn't fit the field's bit width
			result += 4L + ccbits + seg.data.bitLength();
			if (result > Integer.MAX_VALUE)
				return -1;  // The sum will overflow an int type
		}
		return (int)result;
	}
	
	private static final Pattern NUMERIC_REGEX = Pattern.compile("[0-9]*");

	private static final Pattern ALPHANUMERIC_REGEX = Pattern.compile("[A-Z0-9 $%*+./:-]*");
	
	static final String ALPHANUMERIC_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";
	
	public enum Mode {
		NUMERIC     (0x1, 10, 12, 14),
		ALPHANUMERIC(0x2,  9, 11, 13),
		BYTE        (0x4,  8, 16, 16),
		KANJI       (0x8,  8, 10, 12),
		ECI         (0x7,  0,  0,  0);
		
		final int modeBits;
		
		private final int[] numBitsCharCount;

		private Mode(int mode, int... ccbits) {
			modeBits = mode;
			numBitsCharCount = ccbits;
		}
		
		
		/*-- Method --*/
		
		// Returns the bit width of the character count field for a segment in this mode
		// in a QR Code at the given version number. The result is in the range [0, 16].
		int numCharCountBits(int ver) {
			assert QrCode.MIN_VERSION <= ver && ver <= QrCode.MAX_VERSION;
			return numBitsCharCount[(ver + 7) / 17];
		}
		
	}
	
}
