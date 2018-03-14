//
//  SavGolFilter.hpp
//  ImageProcessing
//
//  Created by Renard Wellnitz on 22/02/2018.
//  Copyright © 2018 Renard Wellnitz. All rights reserved.
//

#ifndef SavGolFilter_hpp
#define SavGolFilter_hpp

#include "allheaders.h"
#include "Point.h"

/**
 * \brief Performs a grayscale smoothing using the Savitzky-Golay method.
 *
 * The Savitzky-Golay method is equivalent to fitting a small neighborhood
 * around each pixel to a polynomial, and then recalculating the pixel
 * value from it.  In practice, it achieves the same results without fitting
 * a polynomial for every pixel, so it performs quite well.
 *
 * \param src The source image.  It doesn't have to be grayscale, but
 *        the resulting image will be grayscale anyway.
 * \param window_size The apperture size.  If it doesn't completely
 *        fit the image area, no filtering will take place.
 * \param hor_degree The degree of a polynomial in horizontal direction.
 * \param vert_degree The degree of a polynomial in vertical direction.
 * \return The filtered grayscale image.
 *
 * \note The window size and degrees are not completely independent.
 *       The following inequality must be fulfilled:
 * \code
 *       window_width * window_height >= (hor_degree + 1) * (vert_degree + 1)
 * \endcode
 * Good results for 300 dpi scans are achieved with 7x7 window and 4x4 degree.
 */
Pix* savGolFilter(Pix* src, Point window_size, l_int8 hor_degree, l_int8 vert_degree);

#endif /* SavGolFilter_hpp */
