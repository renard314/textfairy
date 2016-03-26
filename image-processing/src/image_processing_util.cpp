/*  This file is part of Text Fairy.
 
 Text Fairy is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Text Fairy is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with Text Fairy.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * util.cpp
 *
 *  Created on: Mar 4, 2012
 *      Author: renard
 *
 *      Utility functions used during the ocr processing
 */

#include "image_processing_util.h"

#include "allheaders.h"
#include "dewarp_textfairy.h"
#include <sstream>
#include <string>

#include "PixBinarizer.h"
#include "dewarp.h"
#include "pageseg.h"
#include "SkewCorrector.h"

using namespace std;



void bookpage(Pix* pixOrg, Pix** pixText, void (*messageJavaCallback)(int), void (*pixJavaCallback)(Pix*), bool debug) {
    
    Pix *pixb = NULL;
    Pix* pixDewarped = NULL;
    if (debug) {
        startTimer();
    }
    
    PixBinarizer binarizer(false);
    pixb = binarizer.binarize(pixOrg, pixJavaCallback);
    
    if(pixJavaCallback!=NULL){
        pixJavaCallback(pixb);
    }
    
    if (debug>0){
        pixDisplay(pixb,0,0);
        pixWrite("binarized.png", pixb, IFF_PNG);
    }
    
    SkewCorrector skewCorrector(false);
    Pix* pixbRotated = skewCorrector.correctSkew(pixb, NULL);
    pixDestroy(&pixb);
    pixb = pixbRotated;
    
    if(pixJavaCallback!=NULL){
        pixJavaCallback(pixb);
    }
    
    if(messageJavaCallback!=NULL){
        messageJavaCallback(MESSAGE_IMAGE_DEWARP);
    }
    
    startTimer();
    l_int32 dewarpResult = pixDewarp(pixb, &pixDewarped);
    
    if(dewarpResult){
        *pixText = pixClone(pixb);
        log("dewarp failed in: %f",stopTimer());
    } else {
        log("dewarp success in: %f",stopTimer());
        if(pixJavaCallback!=NULL){
            pixJavaCallback(pixDewarped);
        }
        *pixText = pixDewarped;
    }
    pixDestroy(&pixb);
}

/**
 * translate all boxa which are below top dy pixel down
 * translate all boxa which are to the right of left dy pixel to the right
 */
void translateBoxa(Pixa* pixa, l_int32 dx, l_int32 dy, l_int32 left, l_int32 top) {
    log("translateBoxa dx=%i dy=%i, left=%i, top=%i", dx, dy, left, top);
    bool moveDown = dy > 0;
    bool moveRight = dx > 0;
    l_int32 count = pixaGetBoxaCount(pixa);
    if (moveDown || moveRight) {
        //move all boxes on the right side to the right
        //move all boxes on the bottom down
        for (int j = 0; j < count; j++) {
            l_int32 x, y, h, w;
            Box* b = pixaGetBox(pixa, j, L_CLONE);
            boxGetGeometry(b, &x, &y, &w, &h);
            if (moveRight && x > left) {
                log("moving pix %i to %i pixel to right", j, dx);
                x += dx;
            }
            if (moveDown && y > top) {
                log("moving pix %i to %i pixel down", j, dy);
                y += dy;
            }
            boxSetGeometry(b, x, y, w, h);
            boxDestroy(&b);
        }
    }
}

/**
 * destroys all pixa
 */
void combineSelectedPixa(Pixa* pixaText, Pixa* pixaImage, l_int32* textindexes, l_int32 textCount, l_int32* imageindexes, l_int32 imageCount, void (*callbackMessage)(const int), Pix** pPixFinal, Pix** pPixOcr,Boxa** pBoxaColumns, bool debug) {
    ostringstream debugstring;
    
    if (debug) {
        int total = pixaGetCount(pixaText);
        debugstring << total << " Texts";
        if (pixaImage != NULL) {
            total = pixaGetCount(pixaImage);
        }
        debugstring << " and " << total << " Images in total" << endl;
        
        debugstring << textCount << " text indexes: [";
        for (int i = 0; i < textCount; i++) {
            debugstring << textindexes[i];
            if (i < textCount - 1) {
                debugstring << ",";
            }
        }
        debugstring << "]" << std::endl;
        
        debugstring << imageCount << " image indexes: [";
        for (int i = 0; i < imageCount; i++) {
            debugstring << imageindexes[i];
            if (i < imageCount - 1) {
                debugstring << ",";
            }
        }
        debugstring << "]" << std::endl;
    }
    
    //copy marked text pix into new pixa
    Pixa* pixaSelectedColumns = pixaCreate(textCount);
    for (int i = 0; i < textCount; i++) {
        int textIndex = textindexes[i];
        const l_int32 border = 20;
        Pix* p = pixaGetPix(pixaText, textIndex, L_CLONE);
        Pix* p_with_border = pixAddBorder(p, border, 0);
        pixDestroy(&p);
        Box* b = pixaGetBox(pixaText, textIndex, L_CLONE);
        boxAdjustSides(b, b, -border, border, -border, border);
        pixaAddPix(pixaSelectedColumns, p_with_border, L_INSERT);
        pixaAddBox(pixaSelectedColumns, b, L_INSERT);
        int x,y,w,h;
        boxGetGeometry(b,&x,&y,&w,&h);
        translateBoxa(pixaText,border,border,x,y);
    }
    pixaDestroy(&pixaText);
    
    /*dewarp text regions*/
    
    for (int i = 0; i < textCount; i++) {
        int x, y, w, dw, dh, h, dx = 0, dy = 0;
        Pix* pixtext = pixaGetPix(pixaSelectedColumns, i, L_CLONE);
        pixaGetBoxGeometry(pixaSelectedColumns, i, &x, &y, &w, &h);
        Box* b = pixaGetBox(pixaSelectedColumns, i, L_CLONE);
        Pix* pixDewarped = NULL;
        l_int32 dewarpResult = pixDewarp(pixtext, &pixDewarped);
        if(!dewarpResult){
            log("dewarp success");
            dw = pixGetWidth(pixDewarped);
            dh = pixGetHeight(pixDewarped);
            dx = dw - w;
            dy = dh - h;
            boxSetGeometry(b, x, y, dw, dh);
            pixaReplacePix(pixaSelectedColumns, i,pixDewarped, b);
            translateBoxa(pixaSelectedColumns, dx, dy, x, y);
        }
    }
    
    callbackMessage(MESSAGE_ASSEMBLE_PIX);
    
    int xb, yb, wb, hb;
    int left = MAX_INT16;
    int right = 0;
    int top = MAX_INT16;
    int bottom = 0;
    /*get the extend of final pix*/
    for (int i = 0; i < imageCount; i++) {
        int index = imageindexes[i];
        pixaGetBoxGeometry(pixaImage, index, &xb, &yb, &wb, &hb);
        if (left > xb) {
            left = xb;
        }
        if (right < (xb + wb)) {
            right = (xb + wb);
        }
        if (top > yb) {
            top = yb;
        }
        if (bottom < (yb + hb)) {
            bottom = (yb + hb);
        }
    }
    
    int dewarpCount = pixaGetCount(pixaSelectedColumns);
    for (int i = 0; i < dewarpCount; i++) {
        //int index = textindexes[i];
        pixaGetBoxGeometry(pixaSelectedColumns, i, &xb, &yb, &wb, &hb);
        if (left > xb) {
            left = xb;
        }
        if (right < (xb + wb)) {
            right = (xb + wb);
        }
        if (top > yb) {
            top = yb;
        }
        if (bottom < (yb + hb)) {
            bottom = (yb + hb);
        }
    }
    
    l_int32 border = 60;
    l_int32 width = (right - left) + 2 * border;
    l_int32 height = (bottom - top) + 2 * border;
    Pix* pixFinal = pixCreate(width, height, 32);
    pixSetBlackOrWhite(pixFinal, L_SET_WHITE);
    Pix* pixOCR = pixCreate(width, height, 1);
    pixSetBlackOrWhite(pixOCR, L_SET_WHITE);
    int xoffset = left - border;
    int yoffset = top - border;
    
    if (debug) {
        debugstring << "extend of final pix: (" << right << "," << bottom << ")" << endl;
        debugstring << "offset: (" << xoffset << "," << yoffset << ")" << endl;
        startTimer();
    }
    
    /*copy selected pix into final pix*/
    for (int i = 0; i < imageCount; i++) {
        int index = imageindexes[i];
        if (pixaGetBoxGeometry(pixaImage, index, &xb, &yb, &wb, &hb)) {
            continue;
        }
        Pix* pixi = pixaGetPix(pixaImage, index, L_CLONE);
        pixRasterop(pixFinal, xb - xoffset, yb - yoffset, wb, hb, PIX_SRC, pixi, 0, 0);
    }
    printf("%s", "after copying images");
    
    Boxa* boxaColumns = boxaCreate(0);
    for (int i = 0; i < dewarpCount; i++) {
        //int index = textindexes[i];
        if (pixaGetBoxGeometry(pixaSelectedColumns, i, &xb, &yb, &wb, &hb)) {
            continue;
        }
        Pix* pixt = pixaGetPix(pixaSelectedColumns, i, L_CLONE);
        Pix* pixt32 = pixConvertTo32(pixt);
        pixRasterop(pixOCR, xb - xoffset, yb - yoffset, wb, hb, PIX_SRC, pixt, 0, 0);
        pixRasterop(pixFinal, xb - xoffset, yb - yoffset, wb, hb, PIX_SRC, pixt32, 0, 0);
        pixDestroy(&pixt32);
        Box* boxColumn = boxCreate(xb - xoffset, yb - yoffset, wb, hb);
        boxaAddBox(boxaColumns, boxColumn, L_INSERT);
    }
    
    if (pixaImage!=NULL){
        pixaDestroy(&pixaImage);
    }
    
    if (debug) {
        debugstring << "time to assemble final pix: " << stopTimer() << endl;
        printf("%s",debugstring.str().c_str());
    }
    
    *pPixFinal = pixFinal;
    *pPixOcr = pixOCR;
    *pBoxaColumns = boxaColumns;
    printf("%s",debugstring.str().c_str());
}

