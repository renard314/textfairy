//
//  main.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 03/10/15.
//  Copyright © 2015 Renard Wellnitz. All rights reserved.
//

#include <iostream>
#include <sstream>
#include "allheaders.h"
#include "PixBinarizer.h"
#include "PixAdaptiveBinarizer.h"
#include <iostream>
#include <fstream>
#include "binarize.h"
#include "TimerUtil.h"
#include <math.h>
#include "pageseg.h"
#include "image_processing_util.h"
using namespace std;

void binariseTestFolder(const char* images) {
    PixBinarizer binariser(false);
    SARRAY* sa = getSortedPathnamesInDirectory(images, NULL, 0, 0);
    l_int32 count = sarrayGetCount(sa);
    Pix* pixOrg, *pixb;
    char *str;
    for (int i = 0; i < count; i++) {
        str = sarrayGetString(sa, i, L_NOCOPY);
        if ((pixOrg = pixRead(str)) != NULL) {
            printf("binarising %s\n", str);
            pixb = binariser.binarize(pixOrg,NULL);
            ostringstream outFileName;
            std::string path = str;
            std::string base_filename = path.substr(path.find_last_of("/\\") + 1);
            std::string::size_type const p(base_filename.find_last_of('.'));
            std::string file_without_extension = base_filename.substr(0, p);
            outFileName << "binary_" << file_without_extension << ".png";
            Pix* pixGrey = pixConvertTo8(pixOrg, FALSE);
            Pix* pixbCompare;
            binarize(pixGrey, NULL, &pixbCompare);
            Pixa* result = pixaCreate(2);
            pixaAddPix(result, pixOrg, L_CLONE);
            pixaAddPix(result, pixb, L_CLONE);
            pixaAddPix(result, pixbCompare, L_CLONE);
            Pix* pixd = pixaDisplayTiledAndScaled(result, 32, 2048, 3, 0, 25, 2);
            pixWrite(outFileName.str().c_str(), pixd, IFF_PNG);
            //pixWrite(outFileName.str().c_str(),pixb,IFF_PNG);
            pixDestroy(&pixb);
            pixDestroy(&pixd);
            pixDestroy(&pixOrg);
            pixaDestroy(&result);
        }
    }
}

void measurePerformance(const char* image){
    Pix* pixOrg = pixRead(image);
    PixBinarizer binariser(false);
    l_int32 count = 20;
    L_TIMER totalTimer = startTimerNested();
    for(int i=0; i< count; i++){
        Pix* pixb = binariser.binarize(pixOrg,NULL);
        pixDestroy(&pixb);
        
    }
    printf("avg time = %.2f", stopTimerNested(totalTimer)/count);
    pixDestroy(&pixOrg);
    
}


void binariseTest(const char* image) {
    Pix* pixOrg = pixRead(image);
    PixBinarizer binariser(true);
    
    Pix* pixb = binariser.binarize(pixOrg,NULL);
    Pixa* result = pixaCreate(2);
    pixaAddPix(result, pixOrg, L_CLONE);
    pixaAddPix(result, pixb, L_CLONE);
    Pix* pixd = pixaDisplayTiledAndScaled(result, 32, 1024, 2, 0, 25, 2);
    
    pixaDestroy(&result);
    pixWrite("result.png", pixb, IFF_PNG);
    pixWrite("result2.png", pixd, IFF_PNG);
    pixDestroy(&pixd);
    pixDestroy(&pixb);
    pixDestroy(&pixOrg);
}




void testBinary(const char* image) {
    //    Pix* pixOrg = pixRead(image);
    //    PixEdgeDetector edgeDetector;
    //    Pix* pixg = pixConvertTo8(pixOrg,0);
    //    Pix* pixb;
    //    Pix* pixSauvola;
    //    pixSauvolaBinarize(pixg,64, 0.2, 1, NULL, NULL, NULL, &pixSauvola);
    //    pixOtsuAdaptiveThreshold(pixg, 16,16,2,2,0.1, NULL, &pixb);
    //    Pix* outerEdges;
    //    Pix* innerEdges;
    //    Pix* edges = edgeDetector.makeEdges(pixg, &outerEdges,&innerEdges);
    //    l_int32 w,h;
    //    pixGetDimensions(pixOrg, &w, &h, NULL);
    //    Pix* debug = pixCreate(w,h,32);
    //    Pix* pixb8 = pixConvert1To8(NULL, pixb,0, 255);
    //    pixSetRGBComponent(debug, edges, COLOR_BLUE);
    //    pixSetRGBComponent(debug, pixb8, COLOR_GREEN);
    //
    //    pixWrite("pixDebug.png",debug,IFF_PNG);
    //    pixWrite("pixOtsu.png",pixb,IFF_PNG);
    //    pixWrite("pixSauvola.png",pixSauvola,IFF_PNG);
    //
    //    pixWrite("pixEdges.png",edges,IFF_PNG);
    //    pixDestroy(&edges);
    //    pixDestroy(&pixg);
    //    pixDestroy(&pixOrg);
    
}

bool boxTouches2(BOX *box1, BOX *box2) {
    l_int32 l1, l2, r1, r2, t1, t2, b1, b2, w1, h1, w2, h2;
    
    PROCNAME("boxTouches");
    
    if (!box1 || !box2)
        return ERROR_INT("box1 and box2 not both defined", procName, 1);
    
    boxGetGeometry(box1, &l1, &t1, &w1, &h1);
    boxGetGeometry(box2, &l2, &t2, &w2, &h2);
    r1 = l1 + w1;
    r2 = l2 + w2;
    b1 = t1 + h1;
    b2 = t2 + h2;
    if (b2 < t1 || b1 < t2 || r1 < l2 || r2 < l1)
        return false;
    else
        return true;
}

void plotCandidates(){
    Boxa* debugBoxa = boxaRead("debugBoxa.boxa");
    Boxa* invDebugBoxa = boxaRead("invDebugBoxa.boxa");
    Numa* debugAreaFraction = numaRead("areaFraction.numa");
    Numa* invDebugAreaFraction = numaRead("invAreaFraction.numa");
    Numa* debugSizeComp = numaRead("debugSizeComp.numa");
    Numa* invDebugSizeComp = numaRead("invDebugSizeComp.numa");
    
    GPlot* plot =gplotCreate("debug", GPLOT_X11, "debug", "af", "avg");
    
    l_int32 n = numaGetCount(debugAreaFraction);
    Numa* diffAf = numaCreate(n);
    Numa* diffSize = numaCreate(n);
    for(int i = 0; i < n; i++){
        l_float32 af, invAf, size, invSize;
        numaGetFValue(debugAreaFraction, i, &af);
        numaGetFValue(invDebugAreaFraction, i, &invAf);
        numaGetFValue(debugSizeComp, i, &size);
        numaGetFValue(invDebugSizeComp, i, &invSize);
        numaAddNumber(diffAf, af-invAf);
        numaAddNumber(diffSize, size-invSize);
    }
    
    //gplotAddPlot(plot, debugAreaFraction, invDebugSizeComp, GPLOT_POINTS, "org");
    //  gplotAddPlot(plot, invDebugAreaFraction, debugSizeComp, GPLOT_POINTS, "inv");
    //gplotAddPlot(plot, diffAf, diffSize, GPLOT_POINTS, "diffOrg");
    
    
    
    gplotAddPlot(plot, diffAf, diffSize, GPLOT_POINTS, "diff");
    
    
    
    gplotMakeOutput(plot);
    gplotDestroy(&plot);
    
}

l_int32 filterComponent(l_float32 avgDiff, l_float32 avgDiffInv, l_float32 maxDiff, l_float32 maxDiffInv, l_float32 ratioSize, l_float32 ratioSizeInv, l_int32 area1, l_int32 area2, Box* b1, Box* b2, l_int32 expected) {
    //0->candidate1 wins, 1 -> candidate2 wins
    l_int32 result =0;
    l_int32 contains=0;
    l_float32 ratio = 0;
    
    if(area1 > area2){
        boxContains(b1, b2, &contains);
        ratio = ((l_float32)area2)/area1;
    } else {
        boxContains(b2, b1, &contains);
        ratio = ((l_float32)area1)/area2;
    }
    
//    l_int32 minSize = 4;
//    l_float32 areaDiff =  diff-diffInv;
//    l_float32 areaAvg = ((l_float32)(diff+diffInv))/2;
//    l_float32 percentageAreaDiff = L_ABS(areaDiff/areaAvg);
//    
//    if(percentageAreaDiff<0.45){
//        if(ratioSize>ratioSizeInv && area1>50){
//            result = 0;
//        } else if(area2>50){
//            result = 1;
//        } else {
//            if(area1>area2){
//                result = 0;
//            } else {
//                result = 1;
//            }
//        }
//    } else {
//        
//        if(diffInv>diff && area2>minSize && (ratio>0.1 || area2>50)){
//            result = 1;
//        } else if(diffInv<diff && area1>minSize && (ratio>0.1 || area1>50)){
//            result = 0;
//        } else {
//            if(area1>area2){
//                result = 0;
//            } else {
//                result = 1;
//            }
//        }
//    }
    

    l_int32 minSize = 6;
    
    l_float32 scoreInv = (3*maxDiffInv+avgDiffInv)/4.0;
    l_float32 score = (3*maxDiff+avgDiff)/4.0;

    
    if(scoreInv>score && area2>minSize){
        result = 1;
    } else if(area1>minSize){
        result = 0;
    } else {
        if(area1>area2){
            result = 0;
        } else {
            result = 1;
        }
    }
    
    if(expected!=result){
        printf("%.3f - %.2f - %.2f - %i \n%.3f - %.2f - %.2f - %i \ncontains = %i\n", maxDiff, avgDiff, ratioSize, area1 , maxDiffInv, avgDiffInv,ratioSizeInv, area2, contains);
    }
    
    return result;
}

l_int32 runTests(Boxa* debugBoxa, Boxa* invDebugBoxa, Numaa* numaaScoreOrg, Numaa* numaaScoreInv, l_int32 expected){
    l_int32 errors = 0;
    l_int32 count = numaaGetCount(numaaScoreOrg);
    l_int32 boxIndex = 0;
    for(l_int32 documentIndex = 0; documentIndex<count; documentIndex+=3){
        l_int32 componentCount = numaaGetNumaCount(numaaScoreOrg, documentIndex);
        for(l_int32 componentIndex = 0; componentIndex<componentCount; componentIndex++){
            l_float32 avgDiff, avgDiffInv;
            l_float32  maxDiff, maxDiffInv;
            l_float32 ratioSize, ratioSizeInv;
            numaaGetValue(numaaScoreOrg, documentIndex, componentIndex, &avgDiff, NULL);
            numaaGetValue(numaaScoreOrg, documentIndex+1, componentIndex, &maxDiff, NULL);
            numaaGetValue(numaaScoreOrg, documentIndex+2, componentIndex, &ratioSize, NULL);
            
            numaaGetValue(numaaScoreInv, documentIndex, componentIndex, &avgDiffInv, NULL);
            numaaGetValue(numaaScoreInv, documentIndex+1, componentIndex, &maxDiffInv, NULL);
            numaaGetValue(numaaScoreInv, documentIndex+2, componentIndex, &ratioSizeInv, NULL);
            
            Box* b1 = boxaGetBox(debugBoxa,boxIndex, L_CLONE);
            Box* b2 = boxaGetBox(invDebugBoxa,boxIndex++, L_CLONE);
            
            l_int32 w1,h1,w2,h2, area1, area2;
            boxGetGeometry(b1, NULL,NULL, &w1,&h1);
            boxGetGeometry(b2, NULL,NULL, &w2,&h2);
            area1=w1*h1;
            area2=w2*h2;
            l_int32 result = filterComponent(avgDiff, avgDiffInv, maxDiff,maxDiffInv, ratioSize, ratioSizeInv, area1, area2, b1, b2, expected);
            if(result!=expected){
                errors++;
            }
            
            
        }
        
        //printf("%.2f - %.2f - %i \t|\t %.2f - %.2f - %i\tcontains = %i\n", ratioSize, af, area1 , ratioSizeInv, afInv, area2,contains);
    }
    return errors;
    
}

void finetune2(){
    Boxa* debugBoxa = boxaRead("debugBoxa.boxa");
    Boxa* invDebugBoxa = boxaRead("invDebugBoxa.boxa");
    Numaa* numaaScoreOrg = numaaRead("debugScoreOrg.numaa");
    Numaa* numaaScoreInv = numaaRead("debugScoreInv.numaa");
    
//    Numaa* numaaScoreAllInv = numaaRead("allScoresOrg.numaa");
//    l_int32 count = numaaGetCount(numaaScoreAllInv);
//    Numa* numaAvgDiff = numaCreate(0);
//    Numa* numaMaxDiff = numaCreate(0);
//    Numa* numaRatioSize = numaCreate(0);
//
//    for(l_int32 documentIndex = 0; documentIndex<count; documentIndex+=3){
//        l_int32 componentCount = numaaGetNumaCount(numaaScoreAllInv, documentIndex);
//        for(l_int32 componentIndex = 0; componentIndex<componentCount; componentIndex++){
//            l_float32 avgDiffInv;
//            l_float32 maxDiffInv;
//            l_float32 ratioSizeInv;
//            numaaGetValue(numaaScoreAllInv, documentIndex, componentIndex, &avgDiffInv, NULL);
//            numaaGetValue(numaaScoreAllInv, documentIndex+1, componentIndex, &maxDiffInv, NULL);
//            numaaGetValue(numaaScoreAllInv, documentIndex+2, componentIndex, &ratioSizeInv, NULL);
//            numaAddNumber(numaAvgDiff, avgDiffInv);
//            numaAddNumber(numaMaxDiff, maxDiffInv);
//            numaAddNumber(numaRatioSize, ratioSizeInv);
//        }
//    }
//    Numa* sortIndex = numaGetSortIndex(numaMaxDiff, L_SORT_INCREASING);
//    Numa* sortedAvgDiff = numaSortByIndex(numaAvgDiff, sortIndex);
//    Numa* sortedMaxDiff = numaSortByIndex(numaMaxDiff, sortIndex);
//    Numa* sortedRatioSize = numaSortByIndex(numaRatioSize, sortIndex);
//    count = numaGetCount(sortedAvgDiff);
//    printf("avg - max - ratio \n");
//    for(l_int32 i = 0; i< count; i++){
//        l_float32 avgDiffInv;
//        l_float32 maxDiffInv;
//        l_float32 ratioSizeInv;
//        
//        numaGetFValue(sortedAvgDiff, i, &avgDiffInv);
//        numaGetFValue(sortedMaxDiff, i, &maxDiffInv);
//        numaGetFValue(sortedRatioSize, i, &ratioSizeInv);
//        printf("%.3f - %.2f - %.2f \n", maxDiffInv, avgDiffInv, ratioSizeInv);
//    }
    
    
    
    l_int32 errorsBlackOnWhite = runTests(debugBoxa, invDebugBoxa, numaaScoreOrg, numaaScoreInv, 0);
    //    l_int32 errorsWhiteOnBlack = runTests(debugBoxa2, invDebugBoxa2, debugAreaFraction2, invDebugAreaFraction2, debugSizeComp2, invDebugSizeComp2, 1);
    
    l_int32 errorsWhiteOnBlack=0,countWhiteOnBlack = 0;
    
    l_int32 countBlackOnWhite = boxaGetCount(debugBoxa);
    //  l_int32 countWhiteOnBlack = boxaGetCount(debugBoxa2);
    l_int32 errors = errorsBlackOnWhite+errorsWhiteOnBlack;
    printf("B/W ERRORS =%i out of %i\n",errorsBlackOnWhite, countBlackOnWhite);
    //    printf("W/B ERRORS =%i out of %i\n",errorsWhiteOnBlack, countWhiteOnBlack);
    printf("ERRORS =%i out of %i\n",errors,countBlackOnWhite+countWhiteOnBlack);
    
}

void finetune(){
    Boxa* debugBoxa = boxaRead("debugBoxa.boxa");
    Boxa* invDebugBoxa = boxaRead("invDebugBoxa.boxa");
    Numa* debugAreaFraction = numaRead("areaFraction.numa");
    Numa* invDebugAreaFraction = numaRead("invAreaFraction.numa");
    Numa* debugSizeComp = numaRead("debugSizeComp.numa");
    Numa* invDebugSizeComp = numaRead("invDebugSizeComp.numa");
    l_int32 errors = 0;
    
    
    
    
    l_int32 count = boxaGetCount(debugBoxa);
    for(int i=0; i< count; i++){
        Box* b1 = boxaGetBox(debugBoxa,i, L_CLONE);
        Box* b2 = boxaGetBox(invDebugBoxa,i, L_CLONE);
        l_float32 af, afInv, ratio, InvRatio;
        numaGetFValue(debugAreaFraction, i, &af);
        numaGetFValue(invDebugAreaFraction, i, &afInv);
        numaGetFValue(debugSizeComp, i, &ratio);
        numaGetFValue(invDebugSizeComp, i, &InvRatio);
        l_int32 w1,h1,w2,h2, area1, area2;
        boxGetGeometry(b1, NULL,NULL, &w1,&h1);
        boxGetGeometry(b2, NULL,NULL, &w2,&h2);
        area1=w1*h1;
        area2=w2*h2;
        l_float32 score1 = ratio;
        l_float32 score2 = InvRatio;
        //0->candidate1 wins, 1 -> candidate2 wins
        l_int32 result =0;
        l_int32 contains=0;
        
        //if one candidate is very small it can only win if the other canidate has a very low score
        //        printf("%.2f - %.2f - %i \t|\t %.2f - %.2f - %i\tcontains = %i\n", ratio, af, area1 , InvRatio, invAf, area2,contains);
        
        l_int32 smallAreaThresh = 50;
        bool scoreIsSimilar = fabs(score1 - score2)<0.1;
        
        if(score1>score2){
            boxContains(b2, b1, &contains);
            bool isCandidate1VerySmall = area1<smallAreaThresh;
            bool isCandidate2VerySmall = area2<smallAreaThresh;
            bool isCandidate1Small =  area1< area2*0.3;
            bool candidate2HasHighScore = score2>0.40 && afInv>0.25;
            
            if(scoreIsSimilar && contains && af>afInv){
                result = 0;
            } else if(isCandidate1VerySmall && !isCandidate2VerySmall){
                result = 0;
            } else if(isCandidate1VerySmall && isCandidate2VerySmall){
                if(area1>area2){
                    result = 1;
                } else {
                    result = 0;
                }
            } else if(isCandidate1Small && candidate2HasHighScore){
                result = 0;
            } else if(af>afInv && contains && candidate2HasHighScore){
                result = 0;
            } else {
                result = 1;
            }
        } else {
            boxContains(b1, b2, &contains);
            bool isCandidate1VerySmall = area1<smallAreaThresh;
            bool isCandidate2VerySmall = area2<smallAreaThresh;
            bool isCandidate2Small =  area2< area1*0.3;
            bool candidate1HasHighScore = score1>0.40 && af>0.25;
            
            if(scoreIsSimilar && contains && af<afInv){
                result = 1;
            } else if(isCandidate2VerySmall&& !isCandidate1VerySmall){
                result = 1;
            }else if(isCandidate1VerySmall && isCandidate2VerySmall){
                if(area1>area2){
                    result = 1;
                } else {
                    result = 0;
                }
            }  else if(isCandidate2Small && candidate1HasHighScore){
                result = 1;
            } else if(afInv>af && contains && candidate1HasHighScore){
                result = 1;
            } else {
                result = 0;
            }
            
        }
        printf("%.2f - %.2f - %i \t|\t %.2f - %.2f - %i\tcontains = %i\n", ratio, af, area1 , InvRatio, afInv, area2,contains);
        //     printf("%.2f - %.2f - %i \t|\t %.2f - %.2f - %i \t= %i\n", ratio, af, area1 , InvRatio, invAf, area2, result);
        if(result==0){
            errors+=1;
            //            printf("%.2f - %.2f - %i \t|\t %.2f - %.2f - %i \t= %i contains = %i\n", ratio, af, area1 , InvRatio, afInv, area2, result, contains);
        }
    }
    printf("ERRORS =%i\n",errors);
    
    
}

void runBlackOnWhiteData(){
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/109.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/99.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/20.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/21.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/11.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/91.jpeg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/98.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/69.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/5.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/4.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/18.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/8.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/12.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/16.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/100.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/25.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/110.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/last_scan24.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/last_scan34.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/last_scan16.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/last_scan11.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/last_scan7.png");
}

void runWhiteOnBlackData(){
    
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/39.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/41.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/41.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/74.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/85.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/89.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/104.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/105.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/106.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/107.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/108.jpg");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/last_scan10.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/last_scan13.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/last_scan33.png");
    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/last_scan39.png");
}

l_float32 determinePolarityForTile(Pix* pixt, bool debug) {
    l_int32 start = 0, end = 255, error, closeSize = 0;
    
    l_float32 result = 0;
    NUMA* histo = pixGetGrayHistogram(pixt, 1);
    NUMA* norm = numaNormalizeHistogram(histo, 1);
    //    numaSetValue(histo, 255, 0); //ignore white pixels
    error = numaGetNonzeroRange(histo, 0, &start, &end);
    if (end == start || error == 1) {
        numaDestroy(&histo);
        return 0;
    }
    closeSize = end - start;
    if (closeSize % 2 == 0) {
        closeSize++;
    }
    l_int32 splitIndex;
    l_float32 ave1,ave2, num1, num2;
    numaSplitDistribution(histo,0.1,&splitIndex,&ave1,&ave2,&num1, &num2,NULL);
    
    
    
    if (debug == true) {
        printf("split = %i , ave1 = %.2f, num1 = %.2f - ave2=%.2f, num2=%.2f\n", splitIndex, ave1, ave2, num1, num2);
        GPLOT *gplot;
        ostringstream name;
        name << "name";
        gplot = gplotCreate(name.str().c_str(), GPLOT_X11, name.str().c_str(), "x", "y");
        ostringstream title;
        title << "title";
        //gplotAddPlot(gplot, NULL, diff, GPLOT_LINES, "diff histogram");
        gplotAddPlot(gplot, NULL, norm, GPLOT_LINES, title.str().c_str());
        gplotMakeOutput(gplot);
        gplotDestroy(&gplot);
    }
    numaDestroy(&histo);
    
    return result;
}

Pix* findpolarityRegionsTiled(Pix* pixs, const l_uint32 tileSize) {
    L_TIMER timer = startTimerNested();
    l_int32 thresh, w, h;
    ostringstream s;
    pixGetDimensions(pixs, &w, &h, NULL);
    l_int32 nx = L_MAX(1, w / tileSize);
    l_int32 ny = L_MAX(1, h / tileSize);
    l_int32 ox = L_MAX(1,nx/6);
    l_int32 oy = L_MAX(1,ny/6);
    
    PIXTILING* pt = pixTilingCreate(pixs, nx, ny, 0, 0, ox, oy);
    Pix* pixPolarityTiled = pixCreate(w, h, 8);
    Pix* pixt;
    bool debug = false;
    for (int i = 0; i < ny; i++) {
        for (int j = 0; j < nx; j++) {
            pixt = pixTilingGetTile(pt, i, j);
            thresh = determinePolarityForTile(pixt, debug);
            if(thresh==0){
                pixSetAllGray(pixt,50);
            } else {
                pixSetAllGray(pixt,150);
            }
            pixTilingPaintTile(pixPolarityTiled, i, j, pixt, pt);
            pixDestroy(&pixt);
        }
    }
    pixTilingDestroy(&pt);
    s << "local threshhold determination: " << stopTimerNested(timer)
    << std::endl;
    printf("%s", s.str().c_str());
    
    return pixPolarityTiled;
}


void pageSegTest(const char* image) {
    Pix* pixOrg;
    if ((pixOrg = pixRead(image)) != NULL) {
        PixBinarizer binarizer = PixBinarizer(false);
        Pix* pixb = binarizer.binarize(pixOrg, NULL);
        Pixa* pixaText;
        Pixa* pixaImage;
        segmentComplexLayout(pixOrg, NULL, pixb,&pixaImage, &pixaText, NULL, false);
        Boxa* boxaText = pixaGetBoxa(pixaText, L_CLONE);
        
        renderTransformedBoxa(pixOrg, boxaText, 1344);
        pixWrite("columns.png", pixOrg, IFF_PNG);
        
        int textindexes[1]  = {0};
        int textCount = 1;
        int imageindexes[0];
        int imageCount = 0;
        
        Pix* pixFinal;
        Pix* pixOcr;
        Boxa* boxaColumn;
        
        combineSelectedPixa(pixaText, NULL, textindexes, textCount, imageindexes, imageCount, NULL, &pixFinal, &pixOcr, &boxaColumn,true);
        
        pixWrite("combined.png", pixFinal, IFF_PNG);
        boxaDestroy(&boxaText);
        pixDestroy(&pixOrg);
//        pixDestroy(&pixb);
    }
    
}


int main(int argc, const char * argv[]) {
    char * dir = getcwd(NULL, 0);
    cout << "Current dir: " << dir << endl;
    //findpolarityRegions("/Users/renard/devel/textfairy/test-images/binarize/8.jpg");
//    binariseTest("/Users/renard/devel/textfairy/test-images/binarize/last_scan101.jpg");
    //binariseTest("/Users/renard/devel/textfairy/test-images/binarize/11.png");
    //testBinary("/Users/renard/devel/textfairy/test-images/binarize/pixDiff.png");
    //testBinary("/Users/renard/devel/textfairy/test-images/binarize/27.jpg");
    
    pageSegTest("/Users/renard/devel/textfairy/test-images/binarize/last_scan24.png");
//    runBlackOnWhiteData();
//    finetune2();
    //plotCandidates();
    //    runWhiteOnBlackData();
    
//    binariseTestFolder("/Users/renard/devel/textfairy/test-images/binarize");
    //    measurePerformance("/Users/renard/devel/textfairy/test-images/binarize2/73.png");
    //testBinary("/Users/renard/devel/textfairy/CppTestProject/images/binarize/94.jpeg");
    //    testBinary("/Users/renard/devel/textfairy/CppTestProject/images/binarize/91.jpeg");
    return 0;
}
