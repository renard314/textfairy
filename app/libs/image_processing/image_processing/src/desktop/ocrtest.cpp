//
//  ocrtest.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 17/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//


#include "baseapi.h"
#include "osdetect.h"
#include "ocrclass.h"
#include "ccstruct.h"
#include "ocrtest.h"
#include "pixFunc.hpp"
#include <math.h>
#include <sstream>
#include "plot.hpp"

using namespace std;

/**
 * callback for tesseracts monitor
 */
bool cancelFunc(void* cancel_this, int words) {
    return false;
}

/**
 * callback for tesseracts monitor
 */
bool progressJavaCallback(void* progress_this,int progress, int left, int right, int top, int bottom) {
    //printf("progress = %i\n", progress);
    return true;
}

std::string detectLanguage(Pix* pix, l_float32* pConf){
    int orient_deg;
    float orient_conf;
    const char* script_name;
    float script_conf;
    
    tesseract::TessBaseAPI api;
    api.Init("/Users/renard/devel/textfairy/tessdata", "osd", tesseract::OcrEngineMode::OEM_DEFAULT);
    api.SetPageSegMode(tesseract::PageSegMode::PSM_OSD_ONLY);
    api.SetImage(pix);
    api.DetectOrientationScript(&orient_deg, &orient_conf, &script_name, &script_conf);
    api.End();
    
    if(pConf!=NULL){
        *pConf = script_conf;
    }

    return string(script_name);
}

int ocr(Pix* pix, std::string lang){
    tesseract::TessBaseAPI api;
    api.Init("/Users/renard/devel/textfairy/tessdata", lang.c_str(), tesseract::OcrEngineMode::OEM_DEFAULT);
    api.SetPageSegMode(tesseract::PageSegMode::PSM_AUTO);
    api.SetImage(pix);
    
    ETEXT_DESC monitor;
    monitor.progress_callback = progressJavaCallback;
    monitor.cancel = cancelFunc;
    //char* result = api.GetHOCRText(&monitor,0);
    char* utf8Result = api.GetUTF8Text();
    
    int meanConf = api.MeanTextConf();
    
    api.End();
    printf("conf = %i\nResult:\n%s\n",meanConf, utf8Result);
    //free(result);
    free(utf8Result);
    if(meanConf==95){
        meanConf = 0;
    }
    return meanConf;
}

Boxa* analyseLayout(Pix* pix, std::string lang){
    Boxa* result = boxaCreate(0);
    tesseract::TessBaseAPI api;
    api.Init("/Users/renard/devel/textfairy/tessdata", lang.c_str(), tesseract::OcrEngineMode::OEM_DEFAULT);
    api.SetPageSegMode(tesseract::PageSegMode::PSM_AUTO);
    api.SetImage(pix);
    api.Recognize(0);
    tesseract::ResultIterator* ri = api.GetIterator();
    tesseract::PageIteratorLevel level = tesseract::RIL_WORD;
    if (ri != 0) {
        do {
            const char* word = ri->GetUTF8Text(level);
            float conf = ri->Confidence(level);
            int x1, y1, x2, y2;
            ri->BoundingBox(level, &x1, &y1, &x2, &y2);
            boxaAddBox(result, boxCreate(x1, y1, x2-x1, y2-y1), L_INSERT);
            delete[] word;
        } while (ri->Next(level));
    }
    //api.GetIterator()
    Boxa* blocks = api.GetComponentImages(tesseract::PageIteratorLevel::RIL_BLOCK, true, NULL, NULL);
    api.End();
    return blocks;
}




