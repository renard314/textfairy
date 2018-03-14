/*
 * SkewCorrector.cpp
 *
 *  Created on: Jul 8, 2014
 *      Author: renard
 */

#include "skew.h"

l_float32 const DEG_2_RAD = 3.1415926535 / 180.;


Pix* pixCorrectSkew(Pix* pix){
    PROCNAME("pixCorrectSkew");
    
    if(pix==NULL){
        L_ERROR("pixs not defined",procName);
        return NULL;
    }
    l_int32 depth = pixGetDepth(pix);
    if(depth!=1){
        L_WARNING("pixs not 1 bpp",procName);
        return NULL;
    }
    
    l_float32 angle =  0;
    l_int32 error = pixFindSkewSweep(pix, &angle, 2, 47., 1.);
    if (error == 1) {
        return pixClone(pix);
    } else {
        L_INFO("rotating image by %.2f degrees\n", procName, angle);
        return pixRotate(pix, DEG_2_RAD * angle, L_ROTATE_AREA_MAP, L_BRING_IN_WHITE, 0, 0);
    }
}

