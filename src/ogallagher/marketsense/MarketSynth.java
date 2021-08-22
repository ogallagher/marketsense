package ogallagher.marketsense;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Date;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * Converts a 2D dataset to a sound, being a melody of pitches with constant timbre and amplitude using the
 * shape of the input data. The approach is specifically designed with market asset prices in mind.
 * 
 * The resulting sound samples can then be played and saved to audio files.
 * 
 * @author Owen Gallagher
 * @since 2021-08-11
 *
 */
public class MarketSynth {
	private static final double TWO_PI = 2 * Math.PI;
	
	public static final SampleRate SAMPLE_RATE_DEFAULT = SampleRate.MEDIUM;
	/**
	 * Default duration of the sound/melody, in seconds.
	 */
	public static final int SOUND_DURATION_DEFAULT = 2;
	/**
	 * Default number of notes/pitches in the sound/melody.
	 */
	public static final int SOUND_NOTE_COUNT_DEFAULT = 10;
	/**
	 * Lowest comfortable frequency
	 */
	public static final int PITCH_MIN = 120;
	/**
	 * Highest comfortable frequency.
	 */
	public static final int PITCH_MAX = 800;
	/**
	 * {@link PITCH_MAX} - {@link PITCH_MIN}
	 */
	public static final int PITCH_RANGE = PITCH_MAX - PITCH_MIN;
	/**
	 * Default timbre mapping formula.
	 */
	public static final TimbreFormula TIMBRE_FORMULA_DEFAULT = TimbreFormula.HARMONIC_REDOI;
	/**
	 * Default amplitude formula.
	 */
	public static final AmplitudeFormula AMPLITUDE_FORMULA_DEFAULT = AmplitudeFormula.CONST;
	
	public static final String SOUNDS_DIR = "sounds";
	public static final String FILE_EXT = ".wav";
	
	private SampleSize sampleSize = SampleSize.SIXTEEN;
	private SampleRate sampleRate = SAMPLE_RATE_DEFAULT;
	private AudioChannels channels = AudioChannels.MONO;
	boolean signed = true;
	boolean bigEndian = true; // Java is big endian by default
	/**
	 * Encapsulates all the audio data formatting parameters.
	 */
	private AudioFormat audioFormat;
	
	/**
	 * Duration of the sound, in seconds.
	 */
	private int soundDuration = SOUND_DURATION_DEFAULT;
	/**
	 * Maximum allowed amplitude, given the datatype max value representing a single sample.
	 */
	public static int amplitudeMax;
	/**
	 * Fixed amplitude/volume as a proportion of {@link AMPLITUDE_MAX}.
	 */
	private float amplitude = 0.3f;
	private AmplitudeFormula amplitudeFormula = AMPLITUDE_FORMULA_DEFAULT;
	private TimbreFormula timbreFormula = TIMBRE_FORMULA_DEFAULT;
	
	/**
	 * Line that feeds sound to speakers for playback.
	 */
	private SourceDataLine playbackLine = null;
	
	/**
	 * Raw sound data, which can be split into samples and frames for playback.
	 */
	private byte[] soundData;
	/**
	 * Number of frames in the sound.
	 */
	private int frameCount;
	
	private static File soundsDir;
	
	static {
		// ensure sounds dir exists
		String workingDir = MarketSense.class.getResource("./").getPath();
		soundsDir = new File(workingDir, SOUNDS_DIR);
		if (!soundsDir.exists()) {
			soundsDir.mkdir();
			System.out.println("created sounds dir at " + soundsDir.getAbsolutePath());
		}
		else {
			System.out.println("found sounds dir at " + soundsDir.getAbsolutePath());
		}
	}
	
	public MarketSynth() {
		// connect mixer to the proper audio output channel
		audioFormat = new AudioFormat(
			sampleRate.getRate(),
			sampleSize.getSize(), 
			channels.getCount(), 
			signed, 
			bigEndian
		);
		
		if (sampleSize == SampleSize.SIXTEEN) {
			amplitudeMax = Short.MAX_VALUE;
		}
		else {
			amplitudeMax = Byte.MAX_VALUE;
		}
		
		soundData = new byte[
		    sampleRate.getRate() *
		    sampleSize.getByteSize() *
		    channels.getCount() * 
		    soundDuration
		];
		
		frameCount = soundData.length/audioFormat.getFrameSize();
	}
	
	public MarketSynth(TimbreFormula timbreFormula, AmplitudeFormula amplitudeFormula) {
		this();
		this.timbreFormula = timbreFormula;
		this.amplitudeFormula = amplitudeFormula;
	}
	
	public void setTimbreFormula(TimbreFormula timbreFormula) {
		this.timbreFormula = timbreFormula;
	}
	
	public void setAmplitudeFormula(AmplitudeFormula amplitudeFormula) {
		this.amplitudeFormula = amplitudeFormula;
	}
	
	/**
	 * Convert the market data to a sound, accessible via audio input stream. If {@code marketData=null}
	 * then a random sequence of length {@link SOUND_NOTE_COUNT_DEFAULT} is generated.
	 * 
	 * If the market data is already normalized, use with {@code normalize=false}.
	 * 
	 * TODO add a small silence between notes to allow speaker to transition between frequencies.
	 * 
	 * @param marketData
	 * @param normalize Whether to normalize the data (constrain in range {@code [0..1]}), or not. If not,
	 * the data should already be normalized. If {@code marketData=null} then this arg is ignored.
	 */
	public AudioInputStream synthesize(float[] marketData, boolean normalize) {
		// generate market data if not provided
		if (marketData == null) {
			marketData = new float[SOUND_NOTE_COUNT_DEFAULT];
			
			for (int d=0; d<marketData.length; d++) {
				marketData[d] = (float) Math.random();
			}
		}
		else if (normalize) {
			// normalize market data
			float min = marketData[0];
			float max = min;
			
			// find range
			for (int d=1; d<marketData.length; d++) {
				float datum = marketData[d];
				if (datum < min) {
					min = datum;
				}
				else if (datum > max) {
					max = datum;
				}
			}
			float range = max-min;
			
			// constrain
			for (int d=0; d<marketData.length; d++) {
				float raw = marketData[d];
				
				marketData[d] = (raw-min) / range;
			}
		}
		
		// convert to buffer object of bytes or shorts depending on sample size
		Buffer soundBuffer;
		boolean shortNotByte;
		if (sampleSize == SampleSize.EIGHT) {
			soundBuffer = ByteBuffer.wrap(soundData);
			shortNotByte = false;
		}
		else {
			soundBuffer = ByteBuffer.wrap(soundData).asShortBuffer();
			shortNotByte = true;
		}
		
		// write samples to soundBuffer=>soundData
		float pitchRadius = PITCH_RANGE*0.5f;
		float pitchCenter = PITCH_MIN + pitchRadius;
		float amplitudeValue = amplitude * amplitudeMax;
		System.out.println("synth sound with pitch=" + pitchCenter + " amplitude=" + amplitudeValue);
		int rate = sampleRate.getRate();
		
		int noteCount = marketData.length;
		int noteSampleSize = soundBuffer.capacity() / noteCount;
		
		double[] timbre = null;
		double pitch = PITCH_MIN;
		int note = -1;
		int timbreSample = 0;
		
		for (float s=0; s<soundBuffer.capacity(); s++) {
			if (s % noteSampleSize == 0) {
				// move pitch to next note
				
				if (note < marketData.length-1) {
					note++;
				}
				// else if note index is beyond market data, extend last note
				
				pitch = pitchCenter + (marketData[note]-0.5) * pitchRadius;
				
				// create timbre matching input data shape with new length
				int periodSampleSize = (int) (rate / pitch);
				timbre = new double[periodSampleSize];
				
				// write values to timbre
				createTimbre(marketData, amplitudeValue, timbre);
			}
			
			// double time = s/rate;
			
			// select sample from within current timbre
			if (timbreSample >= timbre.length) {
				timbreSample = 0;
			}
			double sample = timbre[timbreSample++];
			
			if (shortNotByte) {
				((ShortBuffer) soundBuffer).put((short) sample); 
			}
			else {
				((ByteBuffer) soundBuffer).put((byte) sample);
			}
	    }
		
		// convert soundData to audio input stream
		AudioInputStream soundStream = new AudioInputStream(
			new ByteArrayInputStream(soundData),
			audioFormat,
			frameCount
		);
		
		return soundStream;
	}
	
	/**
	 * 
	 * @param marketData
	 * @param timbre Output parameter into which timbre values for a single period are written.
	 */
	private void createTimbre(float[] marketData, float amplitude, double[] timbre) {
		double tld = timbre.length;
		for (int t=0; t<timbre.length; t++) {
			double tt;
			int di;
			double tv;
			double theta;
			int oi;
			double den;
			
			switch (timbreFormula) {
				case SINE:
					// pure sine
					tt = t/tld;
					timbre[t] = Math.sin(TWO_PI*tt) * amplitude;
					break;
					
				case ABS_AMP:
					// raw price absolute amplitudes (half amplitude)
					tt = t/tld;
					di = (int) (tt * marketData.length);
					
					timbre[t] = amplitude * marketData[di];
					break;
					
				case MULT:
					// price as sine amplitude multiplier
					tt = t/tld;
					di = (int) (tt * marketData.length);
					
					timbre[t] = Math.sin(TWO_PI*tt) * amplitude * marketData[di];
					break;
					
				case HARMONIC_REDOI:
					// price as harmonic overtone of fundamental pitch with reduced amplitude according to overtone index
					tt = t/tld;
					theta = TWO_PI*tt;
					
					tv = Math.sin(theta) * amplitude;
					
					oi = marketData.length; // overtone index
					den = 2 * marketData.length * marketData.length; // denominator
					for (float dv : marketData) {
						double ra = amplitude * oi / den; // reduced amplitude
						tv += Math.sin(theta + (theta * dv)) * ra;
						oi--;
					}
					
					timbre[t] = tv;
					break;
					
				case MULT_HARMROI:
					// combine MULT and HARMONIC_REDOI
					tt = t/tld;
					di = (int) (tt * marketData.length); // nearest pitch, duration scaled to 1 period
					theta = TWO_PI*tt;
					
					double am = amplitude * marketData[di];
					tv = Math.sin(theta) * am;
					
					oi = marketData.length; // overtone index
					den = 2 * marketData.length * marketData.length; // denominator
					for (float dv : marketData) {
						double ra = am * oi / den; // reduced amplitude
						tv += Math.sin(theta + (theta * dv)) * ra;
						oi--;
					}
					
					timbre[t] = tv;
					break;
			}
		}
	}
	
	/**
	 * Play the given sound a given number of times.
	 */
	public void playback(AudioInputStream soundStream, int repeats) {
		if (playbackLine == null) {
			// retrieve the current playback line
			DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
			try {
				playbackLine = (SourceDataLine) AudioSystem.getLine(lineInfo);
			} 
			catch (LineUnavailableException e) {
				System.out.println("error: no playback line available");
			}
		}
		
		if (playbackLine != null) {
			try {
				soundStream.reset();
				new PlaybackThread(soundStream, playbackLine, repeats).start();
			} 
			catch (IOException e) {
				System.out.println("failed to reset sound stream for playback");
			}
		}
		else {
			System.out.println("speakers not connected; skipping playback");
		}
	}
	
	/**
	 * Same the given sound to a file.
	 * 
	 * @param filePath
	 */
	public void save(AudioInputStream soundStream, String filename) {
		File file = new File(soundsDir.getAbsolutePath(), timbreFormula.toString() + "_" + filename + FILE_EXT);
		
		try {
			soundStream.reset();
			AudioSystem.write(soundStream, AudioFileFormat.Type.WAVE, file);
			System.out.println("saved sound to " + file.getAbsolutePath());
		}
		catch (IOException e) {
			System.out.println("error: failed to save sound to " + file.getAbsolutePath());
		}
	}
	
	public class PlaybackThread extends Thread {
		private AudioInputStream soundStream;
		private SourceDataLine playbackLine;
		private int repeats;
		private byte[] playBuffer;
		
		public PlaybackThread(AudioInputStream soundStream, SourceDataLine playbackLine, int repeats) {
			this.soundStream = soundStream;
			this.playbackLine = playbackLine;
			this.repeats = repeats;
			
			playBuffer = new byte[playbackLine.getBufferSize()];
			System.out.println("init playback line with buffer size " + playBuffer.length);
		}
		
		public void run() {
			try {
				playbackLine.open(audioFormat);
				playbackLine.start();
				
				long start = new Date().getTime();
				
				for (int r=0; r<repeats; r++) {
					int total = 0;
					
					while (total < soundData.length) {
						int newBytes = soundStream.read(playBuffer);
						if (newBytes == -1) break;
						total += newBytes;
						playbackLine.write(playBuffer, 0, newBytes);
					}
					
					soundStream.reset();
				}
				
				playbackLine.drain();
				playbackLine.stop();
				
				long elapsed = new Date().getTime() - start;
				System.out.println("played sound " + repeats + " times for " + elapsed/1000f + " seconds");
			} 
			catch (LineUnavailableException e) {
				System.out.println("speakers no longer available for playback: " + e.getMessage());
			} 
			catch (IOException e) {
				System.out.println("sound read failed: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Allowable sample rates (samples per second): 8000,11025,16000,22050,44100
	 * 
	 * Higher sample rate means higher range of available frequencies.
	 * 
	 * @author Owen Gallagher
	 *
	 */
	public static enum SampleRate {
		LOWEST(8000), 
		LOW(11025),
		MEDIUM(16000),
		HIGH(22050),
		HIGHEST(44100);
		
		private int rate;
		
		/**
		 * @return Number of samples per second (sample rate).
		 */
		public int getRate() {
			return rate;
		}
		
		private SampleRate(int rate) {
			this.rate = rate;
		}
	}
	
	/**
	 * Allowable sample sizes, in bits: 8,16
	 * 
	 * A sample is a single position for the vibrating membrane, and the size of the sample determines the
	 * number of possible positions.
	 * 
	 * @author Owen Gallagher
	 *
	 */
	public static enum SampleSize {
		EIGHT(Byte.SIZE),
		SIXTEEN(Short.SIZE);
		
		private int size;
		private int byteSize;
		
		/**
		 * @return Number of bits.
		 */
		public int getSize() {
			return size;
		}
		
		/**
		 * @return Number of bytes.
		 */
		public int getByteSize() {
			return byteSize;
		}
		
		private SampleSize(int size) {
			this.size = size;
			this.byteSize = size/Byte.SIZE;
		}
	}
	
	public static enum AudioChannels {
		MONO(1),
		STEREO(2);
		
		private int count;
		
		public int getCount() {
			return count;
		}
		
		private AudioChannels(int count) {
			this.count = count;
		}
	}
	
	/**
	 * 
	 * @author Owen Gallagher
	 * @since 2021-08-11
	 *
	 */
	public static enum TimbreFormula {
		/**
		 * Pure sine wave, no timbre.
		 */
		SINE,
		/**
		 * Prices are amplitude multipliers within a period.
		 */
		MULT, 
		/**
		 * Prices are direct amplitudes within a period, and remain 0-1 normalized, staying in
		 * positive range and thus only using half of the available amplitude.
		 */
		ABS_AMP,
		/**
		 * Prices are harmonic overtone pitches that are composed on the fundamental pitch with
		 * reduced amplitudes according to overtone index.
		 */
		HARMONIC_REDOI,
		/**
		 * Combine {@link ABS_AMP} with {@link HARMONIC_REDOI}.
		 */
		MULT_HARMROI;
		
		public String toString() {
			switch (this) {
				case SINE:
					return "sine";
					
				case ABS_AMP:
					return "absamp";
					
				case HARMONIC_REDOI:
					return "harmredoi";
					
				case MULT:
					return "mult";
					
				case MULT_HARMROI:
					return "multharmroi";
					
				default:
					return "unk";
			}
		}
	}
	
	public static enum AmplitudeFormula {
		/**
		 * Constant amplitude.
		 */
		CONST;
		
		public String toString() {
			switch (this) {
				case CONST:
					return "const";
				
				default:
					return "unk";
			}
		}
	}
}
