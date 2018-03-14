//
//  plot.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 24/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#include "plot.hpp"
#include <iostream>
#include <sstream>

l_int32 renderTransformedBoxa(PIX *pixt, BOXA *boxa, l_int32 i) {
    l_int32 j, n, rval, gval, bval;
    BOX *box;
    
    n = boxaGetCount(boxa);
    rval = (1413 * i) % 256;
    gval = (4917 * i) % 256;
    bval = (7341 * i) % 256;
    for (j = 0; j < n; j++) {
        box = boxaGetBox(boxa, j, L_CLONE);
        pixRenderHashBoxArb(pixt, box, 10, 3, i % 4, 1, rval, gval, bval);
        boxDestroy(&box);
    }
    return 0;
}

Numa* numaMakeYNuma(Numa* nax, Numa* nay) {
    l_int32 n = numaGetCount(nax);
    Numa* numaYValues = numaCreate(0);
    for (int i = 0; i < n; i++) {
        l_int32 index;
        l_float32 number;
        numaGetIValue(nax, i, &index);
        
        numaGetFValue(nay, index/nay->delx, &number);
        numaAddNumber(numaYValues, number);
    }
    return numaYValues;
}


void numaPlot(Numa* numa, Numa* numaExtrema, Numa* numaExtrema2, l_int32 outformat, std::string plotName) {
    Numa* numaYValues = numaMakeYNuma(numaExtrema, numa);
    GPLOT *gplot;
    std::ostringstream name;
    std::ostringstream rootName;
    std::ostringstream title;
    rootName<<plotName<<outformat;
    gplot = gplotCreate(rootName.str().c_str(), outformat, name.str().c_str(), "x", "y");
    gplotAddPlot(gplot, NULL, numa, GPLOT_LINES, "numa");
    
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
