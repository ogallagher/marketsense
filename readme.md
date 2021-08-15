# marketsense

Take advantage of the human brain's ability to process signals for pattern recognition and classification by creating a connection from raw data to the human senses, which are the inputs to the brain. Apply this connection to market data and prediction of future asset value.

## Usage

tbd

## Development

tbd

## Approach

> Without having done any real research, below is the approach I'll follow for implementing a first prototype.

The resulting program will be a sort of market analysis training, where the user repeatedly trains by listening to market data converted to sounds, guessing the corresponding color for that sound (which maps to future asset value), and comparing their guess to the actual color. Over time, the user's accuracy score should slowly improve to a point where they can predict future asset value from converted historical market data with high enough accuracy to trade with a profit.

## Technologies

**Java** with **JavaFX** for the GUI and **Maven** for dependency and build management.

**`git submodule`** for using source code from my own modular subpackages: [twelvedata_client_java](https://github.com/ogallagher/twelvedata_client_java), [temp_fx_logger](https://github.com/ogallagher/temp_fx_logger).

[twelvedata market api](https://twelvedata.com/blog/first-introduction-getting-an-advantage-in-a-few-minutes) via custom Java client.

Java Persistence API, with [Hibernate](https://hibernate.org) as the persistence provider, and [H2](https://h2database.com) as the database.

The Java Sound library to synthesize sound with custom [timbre](https://en.wikipedia.org/wiki/Timbre) and [melody](https://en.wikipedia.org/wiki/Melody), along with a controlled [amplitude](https://en.wikipedia.org/wiki/Amplitude) within a comfortable range.

## Questions

### What similar work has been done that uses the brain for data processing?

Examine similar work to determine relevant theories, conclusions, and misconceptions.

- <span id="sight-to-blind-tongue">restoration of sight to the blind by connecting visual data from a sensor and mapping it to another sense, like touch applied to the tongue</span>
- data processing for prediction of weather or market values with a mesh of vibrating nodes worn as a vest over the torso

### Is it necessary to use a sense other than sight? What advantages do other senses have?

In a way, we already attempt to use the human neural network for market data analysis by looking for patterns in visual graphs.

#### Sight

- **absolute** Distinct light frequencies are easily identifiable without reference (ex. green, blue, red, yellow, etc).
- **many dimensions** 2D with depth from binocular vision, and color recognition along the frequency spectrum.

#### Hearing

- **relative** Distance/ratio between sound frequencies are easily identifiable relative to each other, but a pitch is difficult to identify (ex. melody is unchanged by transposition).
- **emotional** High capacity for deriving emotion from different note combinations. In the context of music, different intervals/scales/harmonies/rhythms/etc convey different moods. In speech, the inflection/tones that are spoken can be clues as to the general emotion in the speaker.

Its relativity can recognize contour/shape of a pitch sequence even when its duration, amplitude, and transposition are changed.

#### Smell

tbd

#### Taste

tbd

#### Touch

tbd

### How does the mapping between the data and the human sense affect ability to analyze the data?

#### Learning to _see_ vs learning to _recognize_

It seems like the full power of the brain for analysis comes from what it learns subconsciously/naturally. When _learning_ to see, hear, and use other senses, a person needs no instruction. Rather, instruction is used to label certain patterns, as one would teach the names of things to a child. The name of something exists externally to its appearance (ex: with spoken language, a vision is paired with the sound of a spoken word. ex: with deaf and blind children, the touch of an object is paired with a separate touch of its name written on their hand). In my case, the instruction is needed to label market data with future asset value, perhaps as the percent change in price. The person's brain must then develop a **sense** for the market data, to then recognize a relationship between how the data **looks**/**sounds**/**feels**/etc, and how asset value will change in the near future.

#### How data is mapped and how the output is represented changes the compression relationship between input and output. How does the compression affect analysis?

A potential issue with this application of data analysis with the human brain is that there is heavy compression that needs to happen between the multidimensional input data and the unidimensional output of asset value predicted, or change in asset value. This compression input-output ratio can be controlled by incorporating more/fewer dimensions in the input (current position, historical prices, current profit/loss, proximity to key company events, etc).

When developing [_sight_ via taste/touch](#sight-to-blind-tongue), the subject is connecting the 3D image taken from a camera (x,y,brightness) and converted to tactile sensation on the tongue with the area in the brain that is used to see that image. In that experiment there is little compression (perhaps conversion of RGB to brightness), and rather a convertion to touch, and the person is given time to _see_ it. The analytical aspect is to then be able to apply classification to what is seen (ex. see a banana and learn that it is, in fact, a banana, in terms of the English word and flavor and anything else related to bananas that the person has previously learned).

<h4 id="data-sound-maps">Input data &rarr; sound mappings</h4>

Assume the input data is hourly historical prices of a single stock over a 1-week period.

1. <span id="data-sound-map-1">Minimal mapping; just scale the price line graph so it's within human audible frequency range and listen to the resulting sound.</span>

    - Different inputs will all have the same **pitch**, but different **amplitude** and **voicing**.

2. <span id="data-sound-map-2">Spread the price sequence over a larger playback interval (ex: 5 seconds) and play each price as its own extended pitch within it, so that all pitches are also within human audible frequency range.</span>

    - Prices sound more like a distinct melody together.
    - Different **pitches**, but same **amplitude** and **voicing** for each note.

3. <span id="data-sound-map-3">Combine [1](#data-sound-map-1) and [2](#data-sound-map-2) by controlling the **voicing** and **amplitude** with the overall sequence, and pitches with each price within the sequence.</span>

    - Fully different **pitches**, **amplitudes**, and **voicings**!

### How do different training methods affect ability to classify market data?

Assume a **[data &rarr; sound mapping](#data-sound-maps)** is being used for input data, and a **price change &rarr; color (red to green) mapping** is used for output.

1. Play the sound and **at the end** show the output color corresponding to the future asset value. **Randomize which sound will play next**, ensuring to randomize which security is being played, the time frame it corresponds to, and whether the output color is red or green.

2. Show the **output color while the input sound** is playing. **Randomize sound order**.

3. Chain **sounds as a chronological sequence**, so that each subsequent sound corresponds to the same security as the previous, one step further in time, while the output color is shown continuously for each sound played.

4. Train with repeated sounds to reinforce sound-color matches previously established.

### What are the drawbacks of the human neural network (brain), as opposed to an artificial one (program)?

- Connections in the brain are time sensitive. Over time, unused connections will disappear (memory is imperfect). As such, learning is generally slower.
- Recall/recognition/classification are affected by external variables for a person (mood, diet, rest, etc).

## Authors

- [Owen Gallagher](https://github.com/ogallagher)
