/*
 * TextStatFinder.h
 *
 *  Created on: Jul 8, 2014
 *      Author: renard
 */

#ifndef TEXTSTATFINDER_H_
#define TEXTSTATFINDER_H_


#include <allheaders.h>


class TextStatFinder {
public:
	TextStatFinder(bool debug);

	void getTextStatsTiled(Pix* pix, Numa** textSizes, Pix**  pixTextprobability);
	bool getTextStats(Pix* pix, l_float32* textSize, l_float32* textPropability);
	virtual ~TextStatFinder();

private:
	//calculates the probability that the given tile contains text lines
	bool calculateTextProbability(Pix* pix, l_int32 i, l_int32 j, l_float32* textPropability, l_float32* textSize, l_float32* lineSpacing, bool debug);
	//calculates the text stats and text probability based on the extrema and pixel row sums of a tile
	bool checkTextConditions(Numa* extrema, Numa* numaPixelSum, l_float32* textPropability, l_float32* textSize, bool debug);
	//splits the extrema and pixel row sum numa
	void numaSplitExtrema(Numa* extrema, Numa* numaPixelSum, Numa** peaksX, Numa** peaksY, Numa** valleysX, Numa** valleysY, Numa** peakAreas, Numa** peakAreaWidths, bool debug);
	bool numaGetStdDeviation(Numa* na, l_float32* stdDev, l_float32* errorPercentage, l_float32* mean);
	bool numaGetStdDeviationOnInterval(Numa* na, l_int32 start, l_int32 end, l_float32* stdDev, l_float32* errorPercentage, l_float32* mean);
	void numaGroup(Numa* naTextSize, Numa** groupedMean, Numa** groupedStdDev, Numa** groupedCount);

	//the leptonica implementation ignores the first number
	NUMA * numaMakeDelta2(NUMA *nas);
	//gets the mean width of continuous stretches of 0 values
	l_float32 numaGetMeanHorizontalCrossingWidths(Numa* nay);
	void numaPlot(Numa* numa, Numa* numaExtrema, Numa* numaExtrema2, l_int32 outformat);
	Pix* numaPlotToPix(Numa* numa, Numa* numaExtrema);
	Numa* numaMakeYNuma(Numa* nax, Numa* nay);
	Pix* pixAnnotate(Pix* pixb, const char* textstr);
	void pixaAddPixWithTitle(Pixa* pixa, Pix* pix, const char* title);
	void groupTextSizes(Numa* naTextSize, Numa** groupedTextSizes);

	bool mDebug;

};

#endif /* TEXTSTATFINDER_H_ */

