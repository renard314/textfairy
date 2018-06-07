//
//  pixFunc.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 19/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#include "pixFunc.hpp"
#include "PixBlurDetect.h"
#include "PixEdgeDetector.h"
#include "textsize.hpp"
#include "dewarp_textfairy.h"
#include "skew.h"
#include "savgol.hpp"
#include <cmath>
#include "binarize.h"

#ifdef HAS_ADAPTIVE_BINARIZER
#include "PixBinarizer.h"
#endif


#ifdef ANDROID
#include <android/log.h>

#define printf(fmt,args...)  __android_log_print(ANDROID_LOG_INFO  ,"pixFunc", fmt, ##args)
#define fprintf(file, fmt, args...)  __android_log_print(ANDROID_LOG_INFO  ,"pixFunc", fmt, ##args)
#define FUNCNAME(name)              PROCNAME(name)
#else
#define FUNCNAME(name)              procName = name
char const *procName = NULL;
#endif

Pix* convertTo8(Pix* pix) {
    FUNCNAME("convertTo8");
    return pixConvertTo8(pix, FALSE);
}

Pix* binarizeLegacy(Pix* pix){
    Pix* phm = NULL;
    Pix* pixb = binarizeTiled(pix, phm);
    pixCopyResolution(pixb, pix);
    return pixb;
    
}

#ifdef HAS_ADAPTIVE_BINARIZER
Pix* binarizeAdaptive(Pix* pix) {
    PixAdaptiveBinarizer binarizer(false);
    Pix* pixb =  binarizer.bradleyAdaptiveThresholding(pix, 0.11, 30);
    pixCopyResolution(pixb, pix);
    return pixb;
}
#endif

Pix* binarize(Pix* pix) {
    FUNCNAME("binarize");
    Pix *pixb;
#ifdef HAS_ADAPTIVE_BINARIZER
    PixBinarizer binarizer(false);
    pixb = binarizer.binarize(pix, NULL);
#else
    pixb = binarizeTiled(pix, NULL);
#endif
    pixCopyResolution(pixb, pix);
    return pixb;
}

Pix* binarize(Pix* pix, ProgressCallback* callback) {
    FUNCNAME("binarize");
    Pix *pixb;
#ifdef HAS_ADAPTIVE_BINARIZER
    PixBinarizer binarizer(false);
    pixb = binarizer.binarize(pix, callback);
#else
    pixb = binarizeTiled(pix, NULL);
#endif
    pixCopyResolution(pixb, pix);
    return pixb;
}

Pix* dewarpOrDeskew(Pix* pix) {
    FUNCNAME("dewarpOrDeskew");
    Pix* pixText = NULL;
    l_int32 dewarpResult = pixDewarp(pix, &pixText);
    
    if(dewarpResult){
        L_INFO("dewarp failed. Attempting to correct skew instead.", procName);
        pixText = deskew(pix);
    }
    pixCopyResolution(pixText, pix);
    return pixText;
}

Pix* timePixFunc(Pix* pix, PIX_FUNC pixFunc) {
    PROCNAME("timePixFunc");
    L_TIMER totalTimer = startTimerNested();
    Pix* result = pixFunc(pix);
    l_float32 passedTime = stopTimerNested(totalTimer);
    L_INFO("func time %.2f\n", procName, passedTime);
    return result;
}

Pix* medianFilter(Pix* pix) {
    return pixMedianFilter(pix, 5, 5);
}

Pix* enhance(Pix* pix) {
    return pixBilateral(pix,10,20,6,4);
}

Pix* norm(Pix* pix) {
    return pixBackgroundNormFlex(pix, 6, 6, 2, 2, 25);
}

Pix* unsharpMasking(Pix* pix){
    return pixUnsharpMasking(pix, 7, 0.7f);
}

Pix* upscale(Pix* pix) {
    l_int32 pixPixelCount = pixGetWidth(pix)* pixGetHeight(pix);
    const l_int32 MIN_PIXEL_COUNT = 3 * 1024*1024;
    if (pixPixelCount < MIN_PIXEL_COUNT) {
        l_float32 scale = ((double) MIN_PIXEL_COUNT) / pixPixelCount;
        scale = sqrt(scale);
        Pix* scaled = pixScale(pix, scale,scale);
        l_int32 xres, yres;
        pixGetResolution(pix, &xres, &yres);
        pixSetResolution(scaled, 600, 600);
        return scaled;
    }
    return pixClone(pix);
}

Pix* dynamicRange(Pix* pix){
    return pixGammaTRC(NULL, pix, 1.0, 30, 180);
}

Pix* stats(Pix* pix){
    Numa* histo = pixGetGrayHistogram(pix, 4);
    Numa* norm = numaNormalizeHistogram(histo, 1);
    l_float32 mean, median, variance, stdd;
    numaGetHistogramStats(histo, 0, 1, &mean, &median, NULL, &variance);
    stdd = sqrt(variance);
    printf("stats: mean  = %.2f, stdd = %.2f\n", mean, stdd);
    numaDestroy(&histo);
    numaDestroy(&norm);
    return pixClone(pix);
}

Pix* blurDetect(Pix* pixs){
    PixBlurDetect blurDetector(false);
    l_float32 blurValue;
    Box* maxBlurLoc = NULL;
    Pix* pixBlended = blurDetector.makeBlurIndicator(pixs, &blurValue, &maxBlurLoc);
    boxDestroy(&maxBlurLoc);
    printf("blur = %.2f\n", blurValue);
    return pixBlended;
}

Pix* livreAdapt(Pix* pixs){
    Pix* pixg, *pix1;
    
    /* Normalize for uneven illumination on gray image. */
    pixBackgroundNormGrayArrayMorph(pixs, NULL, 4, 5, 200, &pixg);
    pix1 = pixApplyInvBackgroundGrayMap(pixs, pixg, 4, 4);
    pixDestroy(&pixg);
    
    return pix1;
}

void setResolutionBasedOnTextSize(Pix* pix, l_float32 textLineHeight){
    FUNCNAME("setResolutionBasedOnTextSize");
    l_uint32 res;
    //12pt at 300dpi is roughly 48px
    //since font size is unknown we assume 12pt font
    //48*x = 300. x = 6.25
    
    if(textLineHeight==0){
        l_int32 size = pixGetWidth(pix)*pixGetHeight(pix);
        //1mil==100, 3mill==300
        res = L_MIN(75, L_MAX(300, size/1000));
    } else {
        res = L_MIN(600, 6.25 * textLineHeight);
    }
    L_INFO("text line height = %.2f, dpi = %i\n", procName, textLineHeight, res);
    pixSetResolution(pix, res, res);
}

Pix* findResolution(Pix* pix) {
    FUNCNAME("findResolution");
    l_int32 lines;
    l_float32 textLineHeight = pixGetTextLineHeightGeneral(pix, &lines);
    setResolutionBasedOnTextSize(pix, textLineHeight);
    return pixClone(pix);
}

Pix* pixCorrectOrientation(Pix* pix){
    FUNCNAME("pixCorrectOrientation");
    l_float32 upConf,leftConf;
    l_int32 orientDetectResult = pixOrientDetectDwa(pix, &upConf, &leftConf, 0, 0);
    if(!orientDetectResult){
        l_int32 orient;
        orientDetectResult = makeOrientDecision(upConf, leftConf, 0, 0, &orient, 0);
        if(!orientDetectResult){
            L_INFO("upConf = %.2f, leftConf = %.2f, orientation = %i\n", procName, upConf, leftConf, orient);
        }
        switch (orient) {
            case L_TEXT_ORIENT_UNKNOWN:
            case L_TEXT_ORIENT_UP:
                return pixClone(pix);
                break;
            case L_TEXT_ORIENT_LEFT:
                return pixRotateOrth(pix, 1);
            case L_TEXT_ORIENT_DOWN:
                return pixRotateOrth(pix, 2);
            case L_TEXT_ORIENT_RIGHT:
                return pixRotateOrth(pix, 3);
        }
    }
    return pixClone(pix);
}

Pix* savGol32(Pix* pix){
    FUNCNAME("savGol32");
    Pix* result =  pixSavGolFilter(pix, 3, 2, 2);
    pixCopyResolution(result, pix);
    return result;
}

Pix* savGol(Pix* pix) {
    FUNCNAME("savGol");
    l_int32 xres, yres;
    pixGetResolution(pix, &xres, &yres);
    l_uint8 degree = 4;
    l_float32 textLineHeight = yres/6.25;
    l_uint8 window = L_MIN(15, L_MAX(5, textLineHeight/4));
    if(window % 2 == 0){
        window--;
    }

    L_INFO("text line height = %.2f, window = %i, degree = %i\n", procName, textLineHeight, window, degree);
    Pix* result =  pixSavGolFilter(pix, window, degree, degree);
    pixCopyResolution(result, pix);
    return result;
}

Pix* reduceGray2(Pix* pix){
    FUNCNAME("reduceGray2");
    return pixScaleSmooth(pix, 0.5, 0.5);
}

Pix* reduceGray4(Pix* pix){
    FUNCNAME("reduceGray4");
    return pixScaleSmooth(pix, 0.25, 0.25);
}

Pix* expandGray4(Pix* pix){
    return pixScaleGrayLI(pix, 4, 4);
}

Pix* reduce2(Pix* pix){
    return pixScaleAreaMap2(pix);
}

Pix* scale2Binary(Pix* pix){
    return pixScaleBinary(pix, 2, 2);
}

Pix* edgeDetect(Pix* pix){
    FUNCNAME("edgeDetect");
    PixEdgeDetector edgeDetector;
    return edgeDetector.makeEdges(pix);
}

Pix* invert(Pix* pix){
    FUNCNAME("invert");
    pixInvert(pix,pix);
    return pixClone(pix);
}

Pix* edgeBinarize(Pix* pix){
    FUNCNAME("edgeBinarize");
#if HAS_ADAPTIVE_BINARIZER
    PixAdaptiveBinarizer binarizer(false);
    return binarizer.bradleyAdaptiveThresholding(pix, 0.15, 8);
#endif
    Pix* result;
    pixOtsuAdaptiveThreshold(pix, pixGetWidth(pix), pixGetHeight(pix), 0, 0, 0.1, NULL, &result);
    pixCopyResolution(result, pix);
    return result;
}

Pix* deskew(Pix* pix) {
    return pixCorrectSkew(pix);
}

Pix* ensure150dpi(Pix* pix) {
    FUNCNAME("ensure150dpi");
    l_int32 res = pixGetYRes(pix);
    if(res==0 || res >= 150){
        L_WARNING("resolution (%i) not set or greater than 150 already.\n", procName, res);
        return pixClone(pix);
    }
    l_float32 scale = 150.0/res;
    return pixScaleBinary(pix, scale, scale);
}

Pix* pixPrepareForOcr(Pix* pixOrg, ProgressCallback* callback) {
    auto binarizeWithCallback = [&](Pix* p){
        return binarize(p, callback);
    };
    Pix* result = run(pixOrg, {convertTo8, findResolution, savGol, binarizeWithCallback , ensure150dpi, dewarpOrDeskew});
    FUNCNAME("pixPrepareForOcr");
    return result;
}

Pix* pixPrepareLayoutAnalysis(Pix* pixOrg, ProgressCallback* callback) {
    FUNCNAME("pixPrepareLayoutAnalysis");
    auto binarizeWithCallback = [&](Pix* p){
        return binarize(p, callback);
    };
    return run(pixOrg, {convertTo8, findResolution, savGol, binarizeWithCallback}, callback);
}

Pix* run(Pix* pix, std::list<PIX_FUNC> funcs, ProgressCallback* callback) {
    Pix* pixConverted = pixClone(pix);
    for (auto &func : funcs) {
        Pix* newPix = timePixFunc(pixConverted, func);
        //Pix* newPix = (*it)(pixConverted);
        if(callback!=NULL){
            callback->sendPix(newPix);
        }
        pixDestroy(&pixConverted);
        pixConverted = newPix;
    }
    return pixConverted;
}


Pix* run(Pix* pix, std::list<PIX_FUNC> funcs) {
    Pix* pixConverted = pixClone(pix);
    for (auto &func : funcs) {
        Pix* newPix = timePixFunc(pixConverted, func);
        //Pix* newPix = (*it)(pixConverted);
        pixDestroy(&pixConverted);
        pixConverted = newPix;
    }
    return pixConverted;
}
