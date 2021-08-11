package ogallagher.marketsense.test;

import java.time.LocalDateTime;

import javax.sound.sampled.AudioInputStream;

import ogallagher.marketsense.MarketSynth;
import ogallagher.marketsense.MarketSynth.AmplitudeFormula;
import ogallagher.marketsense.MarketSynth.TimbreFormula;

/**
 * Test market data audio synthesizer.
 * 
 * @author Owen Gallagher
 * @since 2021-08-11
 *
 */
public class TestMarketSynth extends Test {
	private MarketSynth marketSynth;
	
	public TestMarketSynth() {
		marketSynth = new MarketSynth(TimbreFormula.MULT_HARMROI, AmplitudeFormula.CONST);
	}
	
	public void dummySound(boolean persistDummies) {
		// create test sound
		AudioInputStream sound = marketSynth.synthesize(null,false);
		
		if (persistDummies) {
			// save to file
			marketSynth.save(sound, LocalDateTime.now().toString());
		}
		
		// playback
		marketSynth.playback(sound, 4);
	}
	
	@Override
	public void evaluate(boolean persistDummies) {
		System.out.println("testing MarketSynth");
		dummySound(persistDummies);
	}
}
