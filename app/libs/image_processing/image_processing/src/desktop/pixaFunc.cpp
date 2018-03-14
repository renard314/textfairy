//
//  pixaFunc.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 19/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#include "pixaFunc.hpp"
#include "ocrtest.h"

void scriptDetect(Pixa* pixa, string fileName){
    int n = pixaGetCount(pixa);
    
    Pix* page = pixaGetPix(pixa, n-1, L_CLONE);
    l_float32 conf;
    string language = detectLanguage(page, &conf);
    printf("%s = %s (%.2f)\n", fileName.c_str(), language.c_str(), conf);
}

Pix* pixaDisplayDefault(Pixa* pixa){
    return pixaDisplayTiledAndScaled(pixa, 32, 2048, pixaGetCount(pixa), 0, 25, 2);
}

void ignorePixa(Pixa* pixa, string fileName){
    
}

void writeAllPixa(Pixa* pixa, string fileName){
    int n;
    if ((n = pixaGetCount(pixa)) == 0) {
        return;
    }
    
    for (int i = 0; i < n; i++) {
        Pix* pix;
        if ((pix = pixaGetPix(pixa, i, L_CLONE)) == NULL) {
            continue;
        }
        pixWrite((std::to_string(i) + ".png").c_str(), pix, IFF_PNG);
        pixDestroy(&pix);
    }

}

void writeLastPix(Pixa* pixa, string fileName) {
    ostringstream outFileName;
    std::string base_filename = fileName.substr(fileName.find_last_of("/\\") + 1);
    std::string::size_type const p(base_filename.find_last_of('.'));
    std::string file_without_extension = base_filename.substr(0, p);
    outFileName << "last_" << file_without_extension << ".png";
    
    int n = pixaGetCount(pixa);
    Pix* page = pixaGetPix(pixa, n-1, L_CLONE);
    
    pixWrite(outFileName.str().c_str(), page, IFF_PNG);
    pixDestroy(&page);
}


void writePixa(Pixa* pixa, string fileName) {
    ostringstream outFileName;
    std::string base_filename = fileName.substr(fileName.find_last_of("/\\") + 1);
    std::string::size_type const p(base_filename.find_last_of('.'));
    std::string file_without_extension = base_filename.substr(0, p);
    outFileName << "binary_" << file_without_extension << ".png";
    
    Pixa* firstAndLast = pixaCreate(2);
    Pix* page = pixaGetPix(pixa, 0, L_CLONE);
    pixaAddPix(firstAndLast, page, L_INSERT);
    int n = pixaGetCount(pixa);
    page = pixaGetPix(pixa, n-1, L_CLONE);
    pixaAddPix(firstAndLast, page, L_INSERT);
    
    Pix* pix = pixaDisplayDefault(firstAndLast);
    //Pix* pix = pixClone(page);
    pixWrite(outFileName.str().c_str(), pix, IFF_PNG);
    pixDestroy(&pix);
    pixaDestroy(&firstAndLast);
}

void displayPixa(Pixa* pixa, string fileName) {
    //Pix* pix = pixaDisplayDefault(pixa);
    //pixDisplay(pix, 0, 0);
    //pixDestroy(&pix);
    int n;
    if ((n = pixaGetCount(pixa)) == 0) {
        return;
    }
    
    for (int i = 0; i < n; i++) {
        Pix* pix;
        if ((pix = pixaGetPix(pixa, i, L_CLONE)) == NULL) {
            continue;
        }
        pixDisplay(pix, i*50, i*50);
        pixDestroy(&pix);
    }
}
