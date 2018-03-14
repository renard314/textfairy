
#ifndef savgol_hpp
#define savgol_hpp

#include "allheaders.h"

/**
 * taken from scan tailor and adapted for use with leptonica 
 * https://github.com/scantailor/scantailor/blob/67a8466fef752c4dc9ddf409a508e9c3c48a15df/imageproc/SavGolFilter.h
 
 * GPL v3 applies
 * https://www.gnu.org/licenses/gpl-3.0.en.html
 */
Pix* pixSavGolFilter(Pix* pix, l_uint8 window_size, l_uint8 hor_degree, l_uint8 vert_degree );

#endif /* savgol_hpp */
