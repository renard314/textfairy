//
//  main.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 03/10/15.
//  Copyright © 2015 Renard Wellnitz. All rights reserved.
//
#include <iostream>
#include <sstream>
#include <fstream>
#include <unistd.h>
#include "allheaders.h"
#include "PixBinarizer.h"
#include "binarize.h"
#include <cmath>
#include <list>
#include <utility>
#include "ocrtest.h"
#include "RunningStats.h"
#include "pixaFunc.hpp"
#include "pixFunc.hpp"
#include "pageseg.h"
#include "combine_pixa.h"
#include "binarizewolfjolion.hpp"
#include "canny.h"

using namespace std;

Pix* applyToFile(const std::string &fileName, std::list<PIX_FUNC> funcList) {
    Pix* pixOrg;
    if ((pixOrg = pixRead(fileName.c_str())) != NULL) {
        return run(pixOrg, std::move(funcList));
    } else {
        printf("File %s cannot be read.", fileName.c_str());
        return NULL;
    }
}

void applyToFile(const char* fileName, std::list<PIX_FUNC> funcList, PIXA_WRITE_FUNC writeFunc) {
    Pix* pixOrg;
    if ((pixOrg = pixRead(fileName)) != NULL) {
        //printf("processing %s\n", fileName);
        
        Pixa* output = pixaCreate(1);
        pixaAddPix(output, pixOrg, L_INSERT);
        Pix* pixConverted = pixOrg;
        
        for (auto &it : funcList) {
            pixConverted = timePixFunc(pixConverted,(it));
            //pixConverted = it(pixConverted);
            pixaAddPix(output, pixConverted, L_INSERT);
        }
        
        writeFunc(output,fileName);
        pixaDestroy(&output);
    } else {
        printf("File %s cannot be read.", fileName);
    }
}


void applyToFolder(const char* images, const std::list<PIX_FUNC> &funcList, PIXA_WRITE_FUNC writeFunc) {
    SARRAY* sa = getSortedPathnamesInDirectory(images, NULL, 0, 0);
    
    l_int32 count = sarrayGetCount(sa);
    for (int i = 1; i < count; i++) {
        char *fileName = sarrayGetString(sa, i, L_NOCOPY);
        applyToFile(fileName, funcList, writeFunc);
    }
}


l_float32 measurePerformance(Pix* pix, const std::list<PIX_FUNC> &funcList, l_int32 count){
    l_float32 time = 0;
    for(int i=0; i<count; i++){
        L_TIMER totalTimer = startTimerNested();
        Pix* result = run(pix, funcList);
        time+=stopTimerNested(totalTimer);
        pixDestroy(&result);
    }
    return time/count;
}



Numa* confs;
char *fileName;

void ocrPixa(Pixa* pixa, string fileName){
    int n = pixaGetCount(pixa);
    std::string lang = "deu";
    std::size_t lastDot = fileName.find_last_of(".");
    if(lastDot!=std::string::npos){
        std::size_t firstDot = fileName.rfind(".",lastDot-1);
        if(firstDot!=std::string::npos){
            lang = fileName.substr(firstDot+1, lastDot-firstDot-1);
        }
    }
    Pix* page = pixaGetPix(pixa, n-1, L_CLONE);
    int conf = ocr(page, lang);
//    Boxa* layout = analyseLayout(page, lang);
//    renderTransformedBoxa(page,layout, 43);
//    boxaDestroy(&layout);
    //writeAllPixa(pixa, fileName);
    writeLastPix(pixa, fileName);
    printf("%i = %s\n", conf, fileName.c_str());
    if(confs!=NULL){
        numaAddNumber(confs, conf);
    }
    pixDestroy(&page);
}

void compareWithBaseLine(const string &base, string test){
    Numa* numaBaseLine = numaRead(base.c_str());
    Numa* numaTest = numaRead(test.c_str());
    Numa* diff = numaArithOp(NULL, numaTest, numaBaseLine, L_ARITH_SUBTRACT);
    string diffFileName = test + "diff";
    Numa* diffIndex = numaGetSortIndex(diff, L_SORT_INCREASING);
    l_int32 count = numaGetCount(diffIndex);
    for(int i = 0; i<count; i++){
        l_int32 index, conf;
        numaGetIValue(diffIndex, i, &index);
        numaGetIValue(diff, index, &conf);
        printf("%i = %i\n", index, conf );
    }
    
    numaWrite(diffFileName.c_str(), diff);
    l_float32 baselineSum, testSum;
    numaGetSum(numaBaseLine, &baselineSum);
    numaGetSum(numaTest, &testSum);
    printf("baseline average = %.2f/ntest average = %.2f/n",baselineSum/numaGetCount(numaBaseLine), testSum/numaGetCount(numaTest));
}


void comparePipelines(std::string const &fileName, std::list<PIX_FUNC> funcList1, std::list<PIX_FUNC> funcList2 ) {
    Pix* pix1 = applyToFile(fileName, std::move(funcList1));
    Pix* pix2 = applyToFile(fileName, std::move(funcList2));
    pixWrite("org.png", pix1, IFF_PNG);
    pixWrite("new.png", pix2, IFF_PNG);
    pixSubtract(pix1, pix1, pix2);
    pixDisplay(pix1,0, 0);
    pixDestroy(&pix1);
    pixDestroy(&pix2);
}

Pix* comparePerformance(Pix* pix, PIX_FUNC func1, PIX_FUNC func2){
    Pix* pix8 = convertTo8(pix);
    
    l_float32 newTime = measurePerformance(pix8, {func1}, 10);
    l_float32 oldTime = measurePerformance(pix8, {func2}, 10);
    printf("new time = %.2f\nold time = %.2f", newTime, oldTime);
    
    return pix8;
}


void runTests(string testName, string folder, std::list<PIX_FUNC> pixFuncs ){
    confs = numaCreate(0);
    applyToFolder(folder.c_str(), pixFuncs, ocrPixa);
    string fileName = "/Users/renard/devel/textfairy/tests/" + testName;
    numaWrite(fileName.c_str(), confs);
    numaDestroy(&confs);
}

Pix* prepareForOcr(Pix* pix) {
    return pixPrepareForOcr(pix, NULL);
}


Pix* searchThresh(Pix* pix){
    PixAdaptiveBinarizer binarizer(false);
    l_float32 start = 0.01;
    l_float32 end = 0.8;
    l_float32 incr = 0.01;
    l_float32 bestThresh = start;
    int bestConf = 0;
    for(l_float32 thresh = start; thresh<end; thresh+=incr ){
        Pix* result = binarizer.bradleyAdaptiveThresholding(pix, thresh, 15);
        ostringstream name;
        name<<"threshtest "<< thresh<<".png";
        pixWrite(name.str().c_str(), result, IFF_PNG);
        int meanConf = ocr(result, "eng");
        if(meanConf>bestConf){
            bestConf =  meanConf;
            bestThresh = thresh;
        }
        pixDestroy(&result);
    }
    printf("best thresh = %.2f, conf = %i\n", bestThresh, bestConf);
    return binarizer.bradleyAdaptiveThresholding(pix, bestThresh, 15);
}

Pix* pageSegTest(Pix* pix, std::function<Boxa*(Pix*)> pageSegFun) {
    Pix* pix32 = pixConvert1To32(NULL,pix, 0xffffffff, 0);
    Boxa* boxaText = pageSegFun(pix);
    renderTransformedBoxa(pix32, boxaText, 13);
    boxaDestroy(&boxaText);
//    return pix32;

    //pixWrite("columns.png", pix, IFF_PNG);
    
    boxaDestroy(&boxaText);
    return pix32;
}


Pix* pageSegTextFairy(Pix* pix) {
    Pixa* pixaText;
    Pixa* pixaImage;
    Pix* pix32 = pixConvert1To32(NULL,pix, 0xffffffff, 0);
    segmentComplexLayout(pix32, NULL, pix, &pixaImage, &pixaText, NULL, false);
    Boxa* boxaText = pixaGetBoxa(pixaText, L_CLONE);
    renderTransformedBoxa(pix32, boxaText, 13);
    boxaDestroy(&boxaText);
    pixaDestroy(&pixaText);
    pixaDestroy(&pixaImage);
    return pix32;
//    int textindexes[1]  = {0};
//    int textCount = 1;
//    int imageindexes[0];
//    int imageCount = 0;
//    
//    Pix* pixFinal;
//    Pix* pixOcr;
//    Boxa* boxaColumn;
//    
//    combineSelectedPixa(pixaText, pixaImage, textindexes, textCount, imageindexes, imageCount, NULL, &pixFinal, &pixOcr, &boxaColumn,true);
//    pixaDestroy(&pixaText);
//    pixDestroy(&pixOcr);
//    boxaDestroy(&boxaColumn);
//    return pixFinal;
//    

//    return pageSegTest(pix, [&](Pix* pix){
//        Pixa* pixaText;
//        Pixa* pixaImage;
//        Pix* pix32 = pixConvert1To32(NULL,pix, 0xffffffff, 0);
//        segmentComplexLayout(pix32, NULL, pix, &pixaImage, &pixaText, NULL, false);
//        Boxa* boxaText = pixaGetBoxa(pixaText, L_CLONE);
//        pixaDestroy(&pixaText);
//        pixaDestroy(&pixaImage);
//        pixDestroy(&pix32);
//        return boxaText;
//    });
}

Pix* pageSegTess(Pix* pix) {
    return pageSegTest(pix, [&](Pix* pix){
        return analyseLayout(pix, "eng");
    });
}

Pix* pageSegCompare(Pix* pix){
    Pix* tf = pageSegTextFairy(pix);
    Pix* tess = pageSegTess(pix);
    Pixa* combined = pixaCreate(2);
    pixaAddPix(combined, tf, L_INSERT);
    pixaAddPix(combined, tess, L_INSERT);
    Pix* result = pixaDisplayDefault(combined);
    pixaDestroy(&combined);
    return result;
}


Pix* pixCanny(Pix* pix){
    return pixCannyEdge(pix, 0.005, 0.25, false);
}

Pix* expandBinary4(Pix* pix){
    return pixExpandBinaryPower2(pix, 4);
}

int main(int argc, const char * argv[]) {
    //setMsgSeverity(L_SEVERITY_ERROR);
    char * dir = getcwd(NULL, 0);
    cout << "Target dir: " << dir << endl;
    //cout << "Source dir: " << argv[1] << endl;
    delete[] dir;
    
    //applyToFolder("/Users/renard/devel/textfairy/test-images/dewarp", {prepareForOcr}, ocrPixa);
    

    applyToFile("/Users/renard/devel/textfairy/test-images/binarize/0000.png", {prepareForOcr, pageSegTextFairy}, writeLastPix);

    //applyToFile("/Users/renard/devel/textfairy/test-images/dewarp/0014.jpg", {convertTo8, reduceGray4, savGol32, edgeDetect, invert, edgeBinarize, findResolution}, writeLastPix);
    
    
    //applyToFile("/Users/renard/devel/textfairy/test-images/dewarp/0015.jpg", {convertTo8, findResolution, savGol, binarize, dewarpOrDeskew}, ocrPixa);
    
    
    //applyToFile("/Users/renard/devel/textfairy/test-images/dewarp/0007.jpg", {compareSavGolPerformance});
    
    //comparePipelines("/Users/renard/devel/textfairy/test-images/dewarp/0011.jpg", {convertTo8, savGol}, {convertTo8, savGolNew});
    
    //applyToFile("/Users/renard/devel/textfairy/test-images/dewarp/0007.jpg", {convertTo8, savGol, setResolution, binarize, dewarp}, ocrPixa);
    //runTests("dewarpEnsure150Dpi","/Users/renard/devel/textfairy/test-images/dewarp", {prepareForOcr});
    //runTests("dewarpSavGol","/Users/renard/devel/textfairy/test-images/dewarp", {prepareForOcr});
  //  runTests("binarizePageseg","/Users/renard/devel/textfairy/test-images/binarize", {prepareForOcr});
    
    //runTests("dewarp","/Users/renard/devel/textfairy/test-images/dewarp", {convertTo8, binarize, dewarpOrDeskew});
    //compareWithBaseLine("/Users/renard/devel/textfairy/tests/dewarp", "/Users/renard/devel/textfairy/tests/dewarpSavGol");
    //compareWithBaseLine("/Users/renard/devel/textfairy/tests/ocrSavGol", "/Users/renard/devel/textfairy/tests/ocrWolf");
    
    
    return 0;
}
