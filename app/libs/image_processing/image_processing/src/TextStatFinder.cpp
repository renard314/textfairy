/*
 * TextStatFinder.cpp
 *
 *  Created on: Jul 8, 2014
 *      Author: renard
 */

#include "TextStatFinder.h"
#include <sstream>
#include "RunningStats.h"
#include "RunningTextlineStats.h"
#include <math.h>
#include <unistd.h>
#include <vector>
#include "leptonica_legacy.h"

TextStatFinder::TextStatFinder(bool debug) {
	mDebug = debug;
}

TextStatFinder::~TextStatFinder() {
}

Pix* TextStatFinder::pixAnnotate(Pix* pixb, const char* textstr) {
	L_BMF *bmf;
	bmf = bmfCreate("/Users/renard/devel/textfairy/leptonica-1.68/prog/fonts", 4);
	Pix* pixt = pixAddSingleTextblock(pixb, bmf, textstr, 0x00000000, L_ADD_ABOVE, NULL);
	bmfDestroy(&bmf);
	return pixt;
}

void TextStatFinder::pixaAddPixWithTitle(Pixa* pixa, Pix* pix, const char* title) {
	if (title != NULL) {
		Pix* pixt = pixAnnotate(pix, title);
		pixaAddPix(pixa, pixt, L_INSERT);
	} else {
		pixaAddPix(pixa, pix, L_CLONE);
	}
}

Numa* TextStatFinder::numaMakeYNuma(Numa* nax, Numa* nay) {
	l_int32 n = numaGetCount(nax);
	Numa* numaYValues = numaCreate(0);
	for (int i = 0; i < n; i++) {
		l_int32 index;
		l_float32 number;
		numaGetIValue(nax, i, &index);
		numaGetFValue(nay, index, &number);
		numaAddNumber(numaYValues, number);
	}
	return numaYValues;
}

void TextStatFinder::numaPlot(Numa* numa, Numa* numaExtrema, Numa* numaExtrema2, l_int32 outformat) {
	Numa* numaYValues = numaMakeYNuma(numaExtrema, numa);
	GPLOT *gplot;
	std::ostringstream name;
	std::ostringstream rootName;
	std::ostringstream title;
	rootName<<"numaPlot"<<outformat;
	gplot = gplotCreate(rootName.str().c_str(), outformat, name.str().c_str(), "x", "y");
	gplotAddPlot(gplot, NULL, numa, GPLOT_LINESPOINTS, "numa");
	gplotAddPlot(gplot, numaExtrema, numaYValues, GPLOT_IMPULSES, "extrema");
	if(numaExtrema2!=NULL) {
		Numa* numaYValues2 = numaMakeYNuma(numaExtrema2, numa);
		gplotAddPlot(gplot, numaExtrema2, numaYValues2, GPLOT_IMPULSES, "extrema2");
		numaDestroy(&numaYValues2);
	}

	gplotMakeOutput(gplot);
	gplotDestroy(&gplot);
	numaDestroy(&numaYValues);
}

Pix* TextStatFinder::numaPlotToPix(Numa* numa, Numa* numaExtrema) {
	numaPlot(numa, numaExtrema,NULL, GPLOT_PNG);
	usleep(500000);
	return pixRead("numaPLot.png");
}

l_float32 TextStatFinder::numaGetMeanHorizontalCrossingWidths(Numa* nay){
	l_int32 first, last;
	RunningStats stats;
	numaGetNonzeroRange(nay,0,&first, &last);

	l_int32 val;
	l_int32 count = 0;
	for(int i = first; i<last;i++){
		numaGetIValue(nay,i,&val);
		if(val==0){
			count++;
		}
		if(count>0 && val>0){
			stats.Push(count);
			count = 0;
		}
	}
	if(count>0){
		stats.Push(count);
	}
	return stats.Mean();
}

NUMA* TextStatFinder::numaMakeDelta2(NUMA *nas) {
	l_int32 i, n, prev, cur;
	NUMA *nad;
	n = numaGetCount(nas);
	nad = numaCreate(n - 1);
	numaGetIValue(nas, 0, &prev);
	for (i = 1; i < n; i++) {
		numaGetIValue(nas, i, &cur);
		numaAddNumber(nad, cur - prev);
		prev = cur;
	}
	return nad;
}


bool TextStatFinder::numaGetStdDeviationOnInterval(Numa* na, l_int32 start, l_int32 end, l_float32* stdDev, l_float32* errorPercentage, l_float32* mean){
	l_int32 n = numaGetCount(na);
	if (n < 2) {
		return false;
	}
	if(end>n){
		return false;
	}

	l_int32 val;
	RunningStats stats;
	for (int i = start; i < end; i++) {
		numaGetIValue(na, i, &val);
		stats.Push(val);
	}
	if (stdDev != NULL) {
		*stdDev = stats.PopulationStandardDeviation();
	}
	if(errorPercentage!=NULL){
		if(stats.Mean()>0){
			*errorPercentage = stats.PopulationStandardDeviation() / fabs(stats.Mean());
		} else {
			*errorPercentage = 0;
		}
	}
	if(mean!=NULL){
		*mean = stats.Mean();
	}
	return true;
}


bool TextStatFinder::numaGetStdDeviation(Numa* na, l_float32* stdDev, l_float32* errorPercentage, l_float32* mean) {
	l_int32 n = numaGetCount(na);
	return numaGetStdDeviationOnInterval(na,0,n,stdDev,errorPercentage,mean);
}


void TextStatFinder::numaSplitExtrema(Numa* nax, Numa* nay, Numa** peaksX, Numa** peaksY, Numa** valleysX, Numa** valleysY, Numa** peakAreas, Numa** peakAreaWidths, bool debug) {
	Numa* navx;
	Numa* napx;
	Numa* navy;
	Numa* napy;
	Numa* napa;
	Numa* napaw;
	bool isPeakFirst = false, b;
	l_int32 n = numaGetCount(nax);
	l_int32 first, second, peak_count, valley_count, index;
	l_int32 start = 0, end = 0;
	l_float32 valX, valY, sum;

	if (n < 2) {
		return;
	}
	peak_count = n / 2;
	valley_count = peak_count;
	if (n % 2 != 0) {
		numaGetIValue(nax, 0, &index);
		numaGetIValue(nay, index, &first);
		numaGetIValue(nax, 1, &index);
		numaGetIValue(nay, index, &second);
		isPeakFirst = first > second;
		if (isPeakFirst) {
			valley_count--;
		} else {
			peak_count--;
		}
	}
	b = isPeakFirst;
	navx = numaCreate(valley_count);
	napx = numaCreate(peak_count);
	navy = numaCreate(valley_count);
	napy = numaCreate(peak_count);
	napa = numaCreate(peak_count);
	napaw = numaCreate(0);

	for (int i = 0; i < n; i++) {
		numaGetFValue(nax, i, &valX);
		numaGetFValue(nay, valX, &valY);
		if (b) {
			numaAddNumber(napx, valX);
			numaAddNumber(napy, valY);
		} else {
			numaAddNumber(navx, valX);
			numaAddNumber(navy, valY);
		}
		b = !b;
	}

	//calculate the area under the peaks
	n = numaGetCount(nax);
	RunningStats stats;
	l_int32 i = 0;
	while(i<n){

		if(i==0 && isPeakFirst){
			start = 0;
		} else {
			numaGetIValue(nax, i++, &start);
		}
		l_int32 peak;
		numaGetIValue(nax,i,&index);
		numaGetIValue(nay, index, &peak);
		i++;
		if(i==n){
			end = numaGetCount(nay);;
		} else {
			numaGetIValue(nax, i, &end);
		}
		//printf("integrating %i to %i\n",start, end);
		l_int32 error = numaGetSumOnInterval(nay,start,end,&sum);
		if(!error){
			numaAddNumber(napa, sum);
			if(debug){
				//printf("sum = %f, peak = %i, width = %f\n",sum, peak, sum/peak);
			}
			numaAddNumber(napaw, sum/peak);
			if(i>1){
				stats.Push(sum);
			}
		}
	}
	//remove first or last peak area if it differs significantly from the rest (remove  text lines that are cut off)
	l_float32 mean = stats.Mean();
	if(numaGetCount(napa)>1){
		numaGetFValue(napa,0,&sum);
		l_float32 diff = mean-sum;
		l_float32 diffp = diff/mean;
		//if peak area differs from mean by at least 50% remove it
		if(diffp>.3){
			if(debug){
				printf("first peak area differs by =%f%% mean = %f, removing it\n",diffp,mean);
			}
			numaRemoveNumber(napa,0);
			numaRemoveNumber(napaw,0);
		}
	}
	n = numaGetCount(napa);
	if(n>1){
		numaGetFValue(napa,n-1,&sum);
		l_int32 diff = mean-sum;
		//if peak area differs from mean by at least 50% remove it
		if((diff/mean)>.3){
			if(debug){
				printf("removing last peak area diff=%i mean = %f\n",diff,mean);
			}
			numaRemoveNumber(napa,n-1);
			numaRemoveNumber(napaw,n-1);
		}
	}

	*peakAreas = napa;
	*peaksX = napx;
	*peaksY = napy;
	*valleysX = navx;
	*valleysY = navy;
	*peakAreaWidths = napaw;
}

void numasDestroy(std::vector<Numa**> numas) {
	for(std::vector<Numa**>::size_type i = 0; i != numas.size(); i++) {
		numaDestroy(numas[i]);
	}
}




//the original function added crossings twice if the y val was exactly the threshold value
NUMA *
numaCrossingsByThreshold2(NUMA      *nax,
                         NUMA      *nay,
                         l_float32  thresh)
{
l_int32    i, n;
l_float32  startx, delx;
l_float32  xval1, xval2, yval1, yval2, delta1, delta2, crossval, fract;
NUMA      *nad;

    PROCNAME("numaCrossingsByThreshold");

    if (!nay)
        return (NUMA *)ERROR_PTR("nay not defined", procName, NULL);
    n = numaGetCount(nay);

    if (nax && (numaGetCount(nax) != n))
        return (NUMA *)ERROR_PTR("nax and nay sizes differ", procName, NULL);

    nad = numaCreate(0);
    numaGetFValue(nay, 0, &yval1);
    numaGetXParameters(nay, &startx, &delx);
    if (nax)
        numaGetFValue(nax, 0, &xval1);
    else
        xval1 = startx;
    for (i = 1; i < n; i++) {
        numaGetFValue(nay, i, &yval2);
        if (nax) {
            numaGetFValue(nax, i, &xval2);
        } else {
            xval2 = startx + i * delx;
        }
        delta1 = yval1 - thresh;
        delta2 = yval2 - thresh;
        if (delta1 == 0.0) {
        	//printf("delta1 == 0.0\n");
            //numaAddNumber(nad, xval1);
        } else if (delta2 == 0.0) {
            numaAddNumber(nad, xval2);
        } else if (delta1 * delta2 < 0.0) {  /* crossing */
            fract = L_ABS(delta1) / L_ABS(yval1 - yval2);
            crossval = xval1 + fract * (xval2 - xval1);
            numaAddNumber(nad, crossval);
        }
        xval1 = xval2;
        yval1 = yval2;
    }

    return nad;
}

void printNuma2(Numa* na, const char* tag) {
	printf("%s\n", tag);
	l_int32 pc = numaGetCount(na);
	for (int i = 0; i < pc; i++) {
		l_float32 val;
		numaGetFValue(na, i, &val);
		printf("%i = %f\n", i, val);
	}
	printf("\n");
}


/**
 * a pair marks the beginning and the end of a text line
 */
Numa* filterPairs(Numa* extrema, Numa* numaPixelSum) {
	Numa* result = numaCreate(0);
	l_int32 n = numaGetCount(extrema);
	l_float32 first,second, firstY, secondY;
	numaGetFValue(extrema,0,&first);
	numaGetFValue(numaPixelSum,first,&firstY);
	for(int i=1; i<n;i++){
		numaGetFValue(extrema,i,&second);
		numaGetFValue(numaPixelSum,second,&secondY);
		if ((firstY*secondY) <0 ){
			//its a valid pair
			numaAddNumber(result,first);
			numaAddNumber(result,second);
		}
		if (i<n){
			numaGetFValue(extrema,++i,&first);
			numaGetFValue(numaPixelSum,first,&firstY);
		}
	}
	return result;
}

void getLineData(Numa* extrema, Numa** lineHeights, Numa** lineSpacings){
	*lineHeights = numaCreate(0);
	*lineSpacings = numaCreate(0);
	//distance between each peak is the height of a line
	//distance between pairs is the line spacing
	l_int32 n = numaGetCount(extrema);
	l_int32 first,second;
	numaGetIValue(extrema,0,&first);
	for(int i=1; i < n; i++){
		numaGetIValue(extrema,i,&second);
		if(i%2==1){
			numaAddNumber(*lineHeights,second-first);
		} else {
			numaAddNumber(*lineSpacings,second-first);
		}
		first = second;
	}
}

bool TextStatFinder::checkTextConditions(Numa* extrema, Numa* numaPixelSum, l_float32* textPropability, l_float32* textSize, bool debug) {
	//1 group of similar line lengths (peak height)
	//2 group of similar white space lengths (valley height)
	//3 group of similar line heights (area under peak -> line thickness)
	//4 number of crossings is around double the number of peaks
	//5 similar distance between crossings

	Numa* extremaFiltered = filterPairs(extrema,numaPixelSum);
	Numa* lineSpacings, *lineHeights;
	getLineData(extremaFiltered,&lineHeights, &lineSpacings);
	//how much differ the lines in height?
	l_float32 lineHeightError;
	//how much differs the distance between lines?
	l_float32 lineSpacingError;
	numaGetStdDeviation(lineSpacings,NULL,&lineSpacingError,NULL);
	numaGetStdDeviation(lineHeights,NULL,&lineHeightError,NULL);
	//TODO group by spacing or line height
	Numa* lineSpacingMeans, *lineSpacingsStdDevs;
	numaGroup(lineSpacings,&lineSpacingMeans, &lineSpacingsStdDevs,NULL);
	Numa* lineHeightMeans, *lineHeightStdDevs;
	numaGroup(lineHeights,&lineHeightMeans, &lineHeightStdDevs,NULL);
	printNuma2(lineHeightStdDevs,"grouped line height devs");
	printNuma2(lineSpacingsStdDevs,"grouped line spacing devs");
	//printf("line error = %f, spacing error=%f\n",lineHeightError,lineSpacingError);

/*

	bool success = false;
	//split extrema into peaks and valleys
	Numa *px=NULL, *py=NULL, *vx=NULL, *vy=NULL, *pa=NULL, *errors=NULL, *paw=NULL, *peakDelta=NULL, *crossings1=NULL, *crossings2=NULL, *numaX=NULL ;
	std::vector<Numa**> numas;
	numas.push_back(&numaX);
	numas.push_back(&px);
	numas.push_back(&py);
	numas.push_back(&vx);
	numas.push_back(&vy);
	numas.push_back(&pa);
	numas.push_back(&errors);
	numas.push_back(&paw);
	numas.push_back(&peakDelta);
	numas.push_back(&crossings1);
	numas.push_back(&crossings2);

	numaSplitExtrema(extrema, numaPixelSum, &px, &py, &vx, &vy, &pa, &paw, debug);
	errors = numaCreate(5);
	RunningStats stats;

	//check point 1
	//height of peaks corresponds to height of text lines. they should be similar
	l_float32 lineLengthDeviation, lineLengthError = 0;
	l_float32 lineLengthMean;
	success = numaGetStdDeviation(py, &lineLengthDeviation, &lineLengthError,&lineLengthMean);
	if (!success) {
		numasDestroy(numas);
		return false;
	}
	numaAddNumber(errors, lineLengthError);
	stats.Push(lineLengthError);

	//check point 2
	//height valleys should be similar
	l_float32 spacingLengthDeviation, spacingLengthError = 0;
	success = numaGetStdDeviation(vy, &spacingLengthDeviation, &spacingLengthError,NULL);
	if (!success) {
		numasDestroy(numas);
		return false;
	}
	if(lineLengthMean>0){
		spacingLengthError = spacingLengthDeviation/lineLengthMean;
	}
	numaAddNumber(errors, spacingLengthError);
	stats.Push(spacingLengthError);


	//check point 3
	//the distance between lines is indicated by the distance of the peaks which is the delta
	peakDelta = numaMakeDelta2(px);
	l_float32 lineHeightDeviation, lineHeightError;
	success = numaGetStdDeviation(peakDelta, &lineHeightDeviation, &lineHeightError,NULL);
	if (!success) {
		numasDestroy(numas);
		return false;
	}
	lineHeightError=lineHeightError/2;
	numaAddNumber(errors, lineHeightError);
	stats.Push(lineHeightError);


	//still checking point 3 - TODO change to calculated line width instead of peak area
	l_float32 peakAreaDeviation, peakAreaError, peakAreaMean;
	success = numaGetStdDeviation(paw, &peakAreaDeviation, &peakAreaError,&peakAreaMean);
	if (!success) {
		numasDestroy(numas);
		return false;
	}
	printf("peak area mean =%f dev = %f\n",peakAreaMean, peakAreaDeviation);
	//printNuma2(paw,"peak areas");
	numaAddNumber(errors, peakAreaError);
	stats.Push(peakAreaError);

	//check point 4
	//first find the max valley and min peak
	l_float32 maxValleyY, minPeakY;
	l_int32 maxValleyX, minPeakX;
	numaGetMax(vy, &maxValleyY, &maxValleyX);
	numaGetMin(py, &minPeakY, &minPeakX);
	//printf("min peak = %f\nmax valley = %f\n", minPeakY, maxValleyY);
	l_int32 firstCrossingY = maxValleyY + (minPeakY - maxValleyY) / 3;
	l_int32 secondCrossingY = minPeakY - (minPeakY - maxValleyY) / 3;
	//get the crossings
	l_int32 n = numaGetCount(numaPixelSum);
	numaX = numaMakeSequence(0,1,n);
	crossings1 = numaCrossingsByThreshold2(numaX, numaPixelSum, firstCrossingY);
	crossings2 = numaCrossingsByThreshold2(numaX, numaPixelSum, secondCrossingY);
	//number of crossings should be around double the number of peaks
	l_int32 p2 = numaGetCount(px) * 2;
	l_int32 c1n = numaGetCount(crossings1);
	l_int32 c2n = numaGetCount(crossings2);
	l_float32 thresholdCrossingDeviation = sqrt(((p2 - c1n) * (p2 - c1n) + (p2 - c2n) * (p2 - c2n)) / 2);
	l_float32 thresholdCrossingError = thresholdCrossingDeviation/((c1n+c2n)/2);
	numaAddNumber(errors, thresholdCrossingError);
	stats.Push(thresholdCrossingError);


	//check point 6 maybe later
	if(debug) {
		printf("threshold deviation = %f\t\terror = %f (%i at %i,%i at %i)\n", thresholdCrossingDeviation,thresholdCrossingError, c1n,firstCrossingY, c2n, secondCrossingY);
		printf("peak area deviation = %f\t\terror = %f\n", peakAreaDeviation,peakAreaError);
		printf("line distance deviation = %f\terror = %f\n", lineHeightDeviation,lineHeightError);
		printf("spacing length deviation = %f\terror = %f\n", spacingLengthDeviation,spacingLengthError);
		printf("line length deviation = %f\terror = %f\n", lineLengthDeviation,lineLengthError);

	}

	//calculate deviation from optimum values
	//	l_float32 prob = sqrt(((lineLengthError * lineLengthError)*.15 + (spacingLengthError*spacingLengthError)*.15 + (lineHeightError * lineHeightError)*.15 + (peakAreaError * peakAreaError)*35
	//					+ (thresholdCrossingError * thresholdCrossingError)*.2));

	l_float32 totalError = 0;
	for(int i = 0; i < 5; i++){
		l_float32 error;
		numaGetFValue(errors, i,&error);
		//error = 1 - error;
		totalError+= (1 - error)*(1 - error);
	}
	totalError=sqrt(totalError/5);

	if (textPropability != NULL) {
		*textPropability = totalError;
	}
	if(textSize!=NULL){
		RunningStats stats;
		l_int32 w;
		l_int32 n = numaGetCount(paw);
		for(int i = 0; i<n; i++){
			numaGetIValue(paw,i,&w);
			stats.Push(w);
		}
		*textSize = stats.Mean();
	}
	numasDestroy(numas);

*/
	return true;
}

Numa* numaDifferentiate(Numa* na) {
	Numa* result = numaCreate(0);
	l_int32 pc = numaGetCount(na);
	l_float32 prev;
	if(pc>1){
		prev = numaGetFValue(na,0,&prev);
		for (int i = 1; i < pc; i++) {
			l_float32 val;
			numaGetFValue(na, i, &val);
			numaAddNumber(result,prev-val);
			prev = val;
		}
	}
	return result;
}

bool TextStatFinder::calculateTextProbability(Pix* pix, l_int32 i, l_int32 j, l_float32* textPropability, l_float32* textSize, l_float32* lineSpacing, bool debug) {
	Numa* extrema;
	Numa* numaPixelSum;
	Numa* numaClosedPixelSum;
	Pix* pixBorder;
	Pixa* pixaDisplay = pixaCreate(0);
	bool result = false;
	l_int32 extremaCount = 0,tileWidth;
	l_float32 prob = 0;
	if (textPropability != NULL) {
		*textPropability=0;
	}

	pixBorder = pixAddBorder(pix, 1, 0);
	tileWidth = pixGetWidth(pixBorder);
	numaPixelSum = pixSumPixelsByRow(pixBorder, NULL);
	//get width of white area
	l_float32 meanSpacing = numaGetMeanHorizontalCrossingWidths(numaPixelSum);
	*lineSpacing = meanSpacing;
	if(debug){
		printf("mean spacing = %f\n", meanSpacing);
	}
	//numaClosedPixelSum = numaClose(numaPixelSum, floor(meanSpacing/2));
	numaClosedPixelSum = numaClone(numaPixelSum);

	numaClosedPixelSum = numaDifferentiate(numaClosedPixelSum);
	extrema = numaFindExtrema(numaClosedPixelSum, tileWidth / 3);
	extremaCount = numaGetCount(extrema);
	if (extremaCount > 1) {
		result = checkTextConditions(extrema, numaClosedPixelSum, &prob, textSize, debug);
		if (textPropability != NULL) {
			*textPropability = prob;
		}

		if(debug){
			std::ostringstream s;
			s << "tile" << i << j << ".bmp";
			pixWrite(s.str().c_str(), pix, IFF_BMP);
			s.str("");
			s << "chart" << i << j << ".bmp";
			Numa* extremaFiltered = filterPairs(extrema,numaClosedPixelSum);
			numaPlot(numaClosedPixelSum,extrema,extremaFiltered, GPLOT_X11);
			Pix* pixt1 = numaPlotToPix(numaClosedPixelSum, extrema);
			pixWrite(s.str().c_str(), pixt1, IFF_BMP);
			s.str("");
			s << result<<" "<<*textSize;
			pixaAddPixWithTitle(pixaDisplay, pixBorder, s.str().c_str());
			pixaAddPixWithTitle(pixaDisplay, pixt1, "row sums");
			Pix* pixd = pixaDisplayTiledAndScaled(pixaDisplay, 32, 800, 2, 0, 30, 3);
			pixDisplay(pixd, 0, 0);
			pixDestroy(&pixd);
			pixDestroy(&pixt1);
		}
	}
	pixDestroy(&pixBorder);
	pixaDestroy(&pixaDisplay);
	numaDestroy(&extrema);
	numaDestroy(&numaPixelSum);
	numaDestroy(&numaClosedPixelSum);
	return result;
}

bool  TextStatFinder::getTextStats(Pix* pix, l_float32* textSize, l_float32* textPropability){
    PROCNAME("TextStatFinder::getTextStats");
    if(textPropability!=NULL) {
    	*textPropability = 0;
    }
	if(pix==NULL){
		L_WARNING("pixs not defined",procName);
		return false;
	}
	l_int32 depth = pixGetDepth(pix);
	if(depth!=1){
		L_WARNING("pixs not 1 bpp",procName);
		return false;
	}

	l_float32 lineSpacing = 0;
	return calculateTextProbability(pix, 0, 0, textPropability,textSize,&lineSpacing, mDebug);
}

void plotNumaAndPoint(Numa* numa, l_int32 pointIndex){
	GPLOT *gplot;
	std::ostringstream name;
	std::ostringstream rootName;
	gplot = gplotCreate("numaPLotPoint", GPLOT_X11, name.str().c_str(), "x", "y");
	std::ostringstream title;

	l_float32 yval;
	Numa* nax = numaCreate(1);
	Numa* nay = numaCreate(1);
	numaAddNumber(nax, pointIndex);
	numaGetFValue(numa,pointIndex,&yval);
	numaAddNumber(nay,yval);

	gplotAddPlot(gplot, NULL, numa, GPLOT_LINES, "numa");
	gplotAddPlot(gplot, nax, nay, GPLOT_IMPULSES, "point");

	numaDestroy(&nax);
	numaDestroy(&nay);
	gplotMakeOutput(gplot);
	gplotDestroy(&gplot);
}

// groups the array of line heights
void TextStatFinder::numaGroup(Numa* naTextSize, Numa** groupedMean, Numa** groupedStdDev, Numa** groupedCount){
	numaSort(naTextSize,naTextSize,L_SORT_INCREASING);

	l_int32 n = numaGetCount(naTextSize);
	Numa* numaMeanResult = numaCreate(0);
	Numa* numaStdDevResult = numaCreate(0);
	Numa* numaCountResult = numaCreate(0);
	RunningTextlineStats stats(true);
	for (int x = 0; x < n; x++) {
		l_float32 fval;
		numaGetFValue(naTextSize, x, &fval);
		if(stats.Fits(fval)){
			stats.Push(fval);
		} else{
			if (stats.Count()>2){
				printf("\n%i lines grouped, mean = %f\n",stats.Count(),stats.Mean());
				//numaAddNumber(numaCountResult,stats.Count());
				numaAddNumber(numaMeanResult,stats.Mean());
				numaAddNumber(numaStdDevResult,stats.PopulationStandardDeviation());
			}
			stats.Clear();
			stats.Push(fval);
		}
	}

	if (stats.Count()>2){
		printf("%i lines grouped, mean = %f\n",stats.Count(),stats.Mean());
		numaAddNumber(numaCountResult,stats.Count());
		numaAddNumber(numaMeanResult,stats.Mean());
		numaAddNumber(numaStdDevResult,stats.PopulationStandardDeviation());
	}
	if(groupedCount!=NULL){
		*groupedCount = numaCountResult;
	} else {
		numaDestroy(&numaCountResult);
	}

	if(groupedMean!=NULL){
		*groupedMean = numaMeanResult;
	} else{
		numaDestroy(&numaMeanResult);
	}
	if (groupedStdDev!=NULL){
		*groupedStdDev = numaStdDevResult;
	} else {
		numaDestroy(&numaStdDevResult);
	}
}


void TextStatFinder::groupTextSizes(Numa* naTextSize, Numa** groupedTextSizes){
	l_int32 splitIndex,n;
	l_float32 ave1, ave2,stdDev1,stdDev2, mean1, mean2,error;
	n = numaGetCount(naTextSize);
	if(n>1){
		//analyse the array of text sizes
		numaGetStdDeviation(naTextSize,&stdDev1,&error,&mean1);

		numaSort(naTextSize,naTextSize,L_SORT_INCREASING);
		numaSplitDistribution(naTextSize,0,&splitIndex,&ave1, &ave2, NULL, NULL, NULL);
		plotNumaAndPoint(naTextSize,splitIndex);
		bool success = numaGetStdDeviationOnInterval(naTextSize,0,splitIndex-1,&stdDev1,NULL,&mean1);
		if(success){
			numaAddNumber(*groupedTextSizes,mean1);
		}
		success = numaGetStdDeviationOnInterval(naTextSize,splitIndex,n,&stdDev2,NULL,&mean2);
		if(success){
			numaAddNumber(*groupedTextSizes,mean2);
		}
		if(mDebug) {
			printf("textsize 1 = %f, textsize2 = %f\n", mean1, mean2);
		}
	} else if(n==1) {
		numaGetFValue(naTextSize,0,&mean1);
		numaAddNumber(*groupedTextSizes,mean1);
	} else {
		//no text size found
	}
}

void numaPrint(Numa* na, const char* tag) {
	if(na==NULL){
		printf("numa %s is NULL \n", tag);
		return;
	}
	printf("%s\n", tag);
	l_int32 pc = numaGetCount(na);
	for (int i = 0; i < pc; i++) {
		l_float32 val;
		numaGetFValue(na, i, &val);
		printf("%i = %f\n", i, val);
	}
	printf("\n");
}

void  TextStatFinder::getTextStatsTiled(Pix* pix, Numa** textSizes, Pix** pixTextprobability){
    PROCNAME("TextStatFinder::getTextStatsTiled");

	if(pix==NULL){
		L_WARNING("pixs not defined",procName);
		return;
	}
	l_int32 depth = pixGetDepth(pix);
	if(depth!=1){
		L_WARNING("pixs not 1 bpp",procName);
		return;
	}

	PIXTILING* pt = NULL;
	Pixa* pixaDisplay = pixaCreate(0);
	Numa* naLineSpacing = numaCreate(0);
	Numa* naTextSize = numaCreate(0);
	Pix* textProbabilityMap;

	int nx, ny;
	l_float32 tileWidth = pixGetWidth(pix) / 6;
	l_float32 tileHeight = pixGetHeight(pix);
	l_float32 textPropability,textSize, lineSpacing;

	if (tileWidth < 24) {
		tileWidth = 24;
	}
	pt = pixTilingCreate(pix, 0, 0, tileWidth, tileHeight, tileWidth / 2, 0);
	pixTilingGetCount(pt, &nx, &ny);
	textProbabilityMap = pixCreate(nx,ny,8);

	for (int i = 0; i < ny; i++) {
		for (int j = 0; j < nx; j++) {
			Pix* pixt = pixTilingGetTile(pt, i, j);
			pixCloseBrickDwa(pixt,pixt,10,1);
			bool success = calculateTextProbability(pixt, i, j, &textPropability,&textSize, &lineSpacing, true);
			l_float32 p = 0;
			if (success) {
				p = fmaxf(0,textPropability-.8)*1275;

				if(mDebug){
					printf("(%i,%i) - %f, %f",i,j,textPropability,p);
				}
				l_int32 color = 0x00ff0088;
				if(textPropability<.88){
					color = 0xff000088;
				} else {
					numaAddNumber(naLineSpacing, lineSpacing);
					numaAddNumber(naTextSize, textSize);
					if(mDebug){
						printf(" size = %f",textSize);
					}
				}
				if(mDebug){
					printf("\n");
					l_int32 h = pixGetHeight(pixt);
					l_int32 w = pixGetWidth(pixt);
					Box* b = boxCreate(j*w, i*h, w, h);
					boxSetGeometry(b,0,0,w,h);
					std::ostringstream s;
					s<<"("<<i<<","<<j<<") "<<textPropability;
					Pix* pix32  = pixConvert1To32(NULL,pixt,0,0xffffff);
					pixBlendInRect(pix32, b, color, .5f);
					Pix* pixScaled = pixScale(pix32,5,5);
					pixaAddPixWithTitle(pixaDisplay, pixScaled, s.str().c_str());
					boxDestroy(&b);
					pixDestroy(&pixScaled);
					pixDestroy(&pix32);
				}
			} else if(mDebug) {
					printf("(%i,%i) - no Text found\n",i,j);

			}
			pixSetPixel(textProbabilityMap,j,i,p);

			pixDestroy(&pixt);
		}
	}

	if(textSizes!=NULL){
		Numa* textSizeStd=NULL;
		Numa* textSizeCount = NULL;
		numaGroup(naTextSize,textSizes,&textSizeStd, & textSizeCount);
		numaPrint(*textSizes,"text Sizes");
		numaPrint(textSizeStd,"text Sizes std dev");
		numaDestroy(&textSizeStd);
		numaDestroy(&textSizeCount);
	}
	if(mDebug) {
		Pix* pixd = pixaDisplayTiledAndScaled(pixaDisplay, 32, 200, 6, 0, 30, 3);
		pixDisplay(pixd, 0, 0);
		pixDestroy(&pixd);
		//pixWrite("textprobability.bmp", textProbabilityMap, IFF_BMP);
	}
	if(pixTextprobability!=NULL){
		*pixTextprobability = textProbabilityMap;
	} else {
		pixDestroy(&textProbabilityMap);
	}
	numaDestroy(&naTextSize);
	numaDestroy(&naLineSpacing);
	pixaDestroy(&pixaDisplay);
	pixTilingDestroy(&pt);
}



