/*
 * leptonica_legacy.h
 *
 *  Created on: Nov 26, 2014
 *      Author: renard
 */

#ifndef LEPTONICA_LEGACY_H_
#define LEPTONICA_LEGACY_H_
#include <allheaders.h>
/*!
  *  numaGetXParameters()
*
*      Input:  na
*              &startx (<optional return> startx)
*              &delx (<optional return> delx)
*      Return: 0 if OK, 1 on error
*/
 l_int32 numaGetXParameters(NUMA       *na,
                    l_float32  *pstartx,
                    l_float32  *pdelx);


 /*!
  *  pixSumPixelsByRow()
  *
  *      Input:  pix (1, 8 or 16 bpp; no colormap)
  *              tab8  (<optional> lookup table for 1 bpp; use null for 8 bpp)
  *      Return: na of pixel sums by row, or null on error
  *
  *  Notes:
  *      (1) To resample for a bin size different from 1, use
  *          numaUniformSampling() on the result of this function.
  */
 NUMA *
 pixSumPixelsByRow(PIX      *pix, l_int32  *tab8);




#endif /* LEPTONICA_LEGACY_H_ */
