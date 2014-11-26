/*
 * leptonica_legacy.cpp
 *
 *  Created on: Nov 26, 2014
 *      Author: renard
 */

#include "leptonica_legacy.h"

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
                    l_float32  *pdelx)
 {
     PROCNAME("numaGetXParameters");

     if (!na)
         return ERROR_INT("na not defined", procName, 1);

     if (pstartx) *pstartx = na->startx;
     if (pdelx) *pdelx = na->delx;
     return 0;
 }

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
 pixSumPixelsByRow(PIX      *pix,
                   l_int32  *tab8)
 {
 l_int32    i, j, w, h, d, wpl;
 l_uint32  *line, *data;
 l_float32  sum;
 NUMA      *na;

     PROCNAME("pixSumPixelsByRow");

     if (!pix)
         return (NUMA *)ERROR_PTR("pix not defined", procName, NULL);
     pixGetDimensions(pix, &w, &h, &d);
     if (d != 1 && d != 8 && d != 16)
         return (NUMA *)ERROR_PTR("pix not 1, 8 or 16 bpp", procName, NULL);
     if (pixGetColormap(pix) != NULL)
         return (NUMA *)ERROR_PTR("pix colormapped", procName, NULL);

     if (d == 1)
         return pixCountPixelsByRow(pix, tab8);

     if ((na = numaCreate(h)) == NULL)
         return (NUMA *)ERROR_PTR("na not made", procName, NULL);
     data = pixGetData(pix);
     wpl = pixGetWpl(pix);
     for (i = 0; i < h; i++) {
         sum = 0.0;
         line = data + i * wpl;
         if (d == 8) {
             sum += w * 255;
             for (j = 0; j < w; j++)
                 sum -= GET_DATA_BYTE(line, j);
         }
         else {  /* d == 16 */
             sum += w * 0xffff;
             for (j = 0; j < w; j++)
                 sum -= GET_DATA_TWO_BYTES(line, j);
         }
         numaAddNumber(na, sum);
     }

     return na;
 }






