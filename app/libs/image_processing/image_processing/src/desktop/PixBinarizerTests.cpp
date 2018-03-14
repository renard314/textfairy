//
//  PixBinarizerTests.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 08/03/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#include "PixBinarizerTests.hpp"
#include "allheaders.h"
#include <cmath>

void plotCandidates(){
    Boxa* debugBoxa = boxaRead("debugBoxa.boxa");
    Boxa* invDebugBoxa = boxaRead("invDebugBoxa.boxa");
    Numa* debugAreaFraction = numaRead("areaFraction.numa");
    Numa* invDebugAreaFraction = numaRead("invAreaFraction.numa");
    Numa* debugSizeComp = numaRead("debugSizeComp.numa");
    Numa* invDebugSizeComp = numaRead("invDebugSizeComp.numa");
    
    GPlot* plot =gplotCreate("debug", GPLOT_PNG, "debug", "af", "avg");
    
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
    
    //    l_float32 scoreInv = (3*maxDiffInv+avgDiffInv)/4.0;
    //    l_float32 score = (3*maxDiff+avgDiff)/4.0;
    l_float32 scoreInv = maxDiffInv*avgDiffInv;
    l_float32 score = maxDiff*avgDiff;
    
    
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
