//
//  PixEdgeDetector.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 20/12/15.
//  Copyright © 2015 Renard Wellnitz. All rights reserved.
//

#include "PixEdgeDetector.h"


PIX * PixEdgeDetector::makeEdges(PIX * pixs) {
    
    l_int32 w, h, d, i, j, wplt, wpld;
    l_int32 val1, val2, val3, val4 = 0, val5 = 0, val6 = 0, val7 = 0, val8 = 0, val9 = 0;
    l_uint32 *datat, *linet, *datad, *lined;
    PIX *pixt, *pixd;
    
    PROCNAME("PixEdgeDetector::makeEdges");
    
    if (!pixs) {
        return (PIX *) ERROR_PTR("pixs not defined", procName, NULL);
    }
    
    pixGetDimensions(pixs, &w, &h, &d);
    if (d != 8) {
        return (PIX *) ERROR_PTR("pixs not 8 bpp", procName, NULL);
    }
    /* Add 1 pixel (mirrored) to each side of the image. */
    if ((pixt = pixAddMirroredBorder(pixs, 1, 1, 1, 1)) == NULL)
        return (PIX *) ERROR_PTR("pixt not made", procName, NULL);
    
    /* Compute filter output at each location. */
    pixd = pixCreateTemplate(pixs);
    datat = pixGetData(pixt);
    wplt = pixGetWpl(pixt);
    datad = pixGetData(pixd);
    wpld = pixGetWpl(pixd);
    l_int32 wplt2 = wplt * 2;
    
    for (i = 0; i < h; i++) {
        linet = datat + i * wplt;
        lined = datad + i * wpld;
        for (j = 0; j < w; j++) {
            if (j == 0) { /* start a new row */
                val1 = GET_DATA_BYTE(linet, j);
                val2 = GET_DATA_BYTE(linet + wplt, j);
                val3 = GET_DATA_BYTE(linet + (wplt << 1), j);
                val4 = GET_DATA_BYTE(linet, j + 1);
                val5 = GET_DATA_BYTE(linet + wplt, j + 1);
                val6 = GET_DATA_BYTE(linet + (wplt << 1), j + 1);
                val7 = GET_DATA_BYTE(linet, j + 2);
                val8 = GET_DATA_BYTE(linet + wplt, j + 2);
                val9 = GET_DATA_BYTE(linet + (wplt << 1), j + 2);
            } else { /* shift right by 1 pixel; update incrementally */
                val1 = val4;
                val2 = val5;
                val3 = val6;
                val4 = val7;
                val5 = val8;
                val6 = val9;
                l_int32 jPlus2 = j + 2;
                val7 = GET_DATA_BYTE(linet, jPlus2);
                val8 = GET_DATA_BYTE(linet + wplt, jPlus2);
                val9 = GET_DATA_BYTE(linet + wplt2, jPlus2);
            }
            l_int32 val2By10 = (val2 << 3) + (val2 << 1);
            l_int32 val8By10 = (val8 << 3) + (val8 << 1);
            l_int32 val4By10 = (val4 << 3) + (val4 << 1);
            l_int32 val6By10 = (val6 << 3) + (val6 << 1);
            l_int32 val1By3 = (val1 << 1) + val1;
            l_int32 val3By3 = (val3 << 1) + val3;
            l_int32 val7By3 = (val7 << 1) + val7;
            l_int32 val9By3 = (val9 << 1) + val9;
            
            l_int32 val1MinusVal9 = val1By3 - val9By3;
            l_int32 val3MinusVal7 = val3By3 - val7By3;
            l_int32 gx = val1MinusVal9 + val3MinusVal7 + val2By10 - val8By10;
            l_int32 gy = val1MinusVal9 - val3MinusVal7 + val4By10 - val6By10;
            l_uint32 absGx = L_ABS(gx);
            l_uint32 absGy = L_ABS(gy);
            
            l_uint32 vald = L_MIN(255, (absGy + absGx) >> 2);
            SET_DATA_BYTE(lined, j, vald);
        }
    }
    
    pixDestroy(&pixt);
    return pixd;
}