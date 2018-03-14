//
//  savgol.cpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 04/03/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#include "savgol.hpp"
#include "SavGolKernel.hpp"

Pix* pixSavGolFilter(Pix* pix, l_uint8 window_size, l_uint8 hor_degree, l_uint8 vert_degree ){
    
    SavGolKernel const hor_kernel(Point(window_size, 1), Point(window_size/2, 0), hor_degree, 0);
    SavGolKernel const vert_kernel(Point(1, window_size), Point(0, window_size/2), 0, vert_degree);
    
    L_KERNEL*  hk = kernelCreate(window_size, 1);
    kernelSetOrigin(hk, window_size/2, 0);
    
    for (int i = 0; i < window_size; i++) {
        kernelSetElement(hk, i, 0, hor_kernel[i]);
    }
    
    L_KERNEL*  vk = kernelCreate(1, window_size);
    kernelSetOrigin(vk, 0, window_size/2);
    
    for (int i = 0; i < window_size; i++) {
        kernelSetElement(vk, 0, i, vert_kernel[i]);
    }
    
    Pix* pixd = pixConvolveSep(pix, hk, vk, 8, 0);
    
    kernelDestroy(&hk);
    kernelDestroy(&vk);
    
    return pixd;
    
}
