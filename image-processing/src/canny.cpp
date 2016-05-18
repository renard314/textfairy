/*
 * canny.cpp
 *
 *  Created on: Jul 18, 2015
 *      Author: renard
 */

#include "canny.h"
#include <cmath>

#define MagickEpsilon  (1.0e-15)

Pix* pixCannyEdge(Pix* pixs, l_float32 lower_percent, l_float32 upper_percent, bool debug) {
	l_int32 nx = pixGetWidth(pixs);
	l_int32 ny = pixGetHeight(pixs);
	Pix* orientation = pixCreate(nx, ny, 8);
	Pix* edges = pixCreate(nx, ny, 8);
	Pix* pixNms = pixCreate(nx, ny, 8);
	//Pix* pixt = pixAddMirroredBorder(pixs, 1, 1, 1, 1);

	l_uint32* datas = pixGetData(pixs);
	l_int32 wpls = pixGetWpl(pixs);
	l_uint32* datae = pixGetData(edges);
	l_int32 wple = pixGetWpl(edges);
	l_int32 wpl = pixGetWpl(edges);
	l_uint32* datao = pixGetData(orientation);
	l_int32 wplo = pixGetWpl(orientation);
	l_uint32* datanms = pixGetData(pixNms);
	l_int32 wplnms = pixGetWpl(pixNms);
	l_uint32* out, *c, *nms, *oLine;
	l_uint8 min = 255;
	l_uint8 max = 0;
	L_TIMER timer = startTimerNested();
	for (int i = 0; i < ny - 1; i++) {
		c = datas + i * wpls;
		out = datae + i * wple;
		oLine = datao + i * wplo;
		for (int j = 0; j < nx - 1; j++) {
			l_int32 dx = 0.0;
			l_int32 dy = 0.0;
			l_uint8 v1 = GET_DATA_BYTE(c, j);
			l_uint8 v2 = GET_DATA_BYTE(c, j + 1);
			l_uint8 v3 = GET_DATA_BYTE(c + wpls, j);
			l_uint8 v4 = GET_DATA_BYTE(c + wpls, j + 1);

			dx -= v1;
			dx += v2;
			dx -= v3;
			dx += v4;

			dy += v1;
			dy += v2;
			dy -= v3;
			dy -= v4;
			//l_uint8 vald = L_MIN(255, hypotf(dx, dy));
			l_uint8 vald = L_MIN(255, abs(dx) + abs(dy));
			SET_DATA_BYTE(out, j, vald);
			l_uint8 orientation = 0;
			if (abs(dx) > MagickEpsilon) {
				double slope;

				slope = dy / dx;
				if (slope < 0.0) {
					if (slope < -2.41421356237)
						orientation = 0;
					else if (slope < -0.414213562373)
						orientation = 64;
					else
						orientation = 128;
				} else {
					if (slope > 2.41421356237)
						orientation = 0;
					else if (slope > 0.414213562373)
						orientation = 192;
					else
						orientation = 128;
				}
			}
			SET_DATA_BYTE(oLine, j, orientation);
		}
	}
	printf("canny edge =%f\n", stopTimerNested(timer));
	if (debug) {
		pixWrite("orientation.png", orientation, IFF_PNG);
		pixWrite("edges.png", edges, IFF_PNG);
	}
	timer = startTimerNested();

	//non maximum supression
	for (int i = 1; i < ny - 1; i++) {
		c = datae + i * wple;
		oLine = datao + i * wplo;
		out = datanms + i * wplnms;
		for (int j = 1; j < nx - 1; j++) {
			l_uint8 a, b, val;
			val = GET_DATA_BYTE(c, j);
			if (val < 10) {
				continue;
			}
			l_uint8 orientation = GET_DATA_BYTE(oLine, j);
			switch (orientation) {
			default:
			case 0: {
				/*
				 0 degrees, north and south
				 */
				a = GET_DATA_BYTE(c - wpl, j);
				b = GET_DATA_BYTE(c + wpl, j);
				break;
			}
			case 64: {
				/*
				 45 degrees, northwest and southeast.
				 */
				a = GET_DATA_BYTE(c - wpl, j - 1);
				b = GET_DATA_BYTE(c + wpl, j + 1);
				break;
			}
			case 128: {
				/*
				 90 degrees, east and west
				 */
				a = GET_DATA_BYTE(c, j - 1);
				b = GET_DATA_BYTE(c, j + 1);
				break;
			}
			case 192: {
				/*
				 135 degrees, northeast and southwest.
				 */
				a = GET_DATA_BYTE(c - wpl, j + 1);
				b = GET_DATA_BYTE(c + wpl, j - 1);
				break;
			}
			}
			//if(print){
			//printf("(%i,%i) - o=%i, val=%i, a=%i, b=%i\n",j,i,orientation,val, a,b);
			//}
			if ((val < a) || (val < b)) {
				val = 0;
			} else {
				SET_DATA_BYTE(out, j, val);
				if (val < min) {
					min = val;
				}
				if (val > max) {
					max = val;
				}
			}
		}
	}
	if (debug) {
		pixWrite("nms.png", pixNms, IFF_PNG);
	}
	printf("canny nms=%f\n", stopTimerNested(timer));
	timer = startTimerNested();

	/*
	 Estimate hysteresis threshold.
	 */
	Numa* histo = pixGetGrayHistogram(pixNms, 8);
	numaSetValue(histo, 0, 0);
	l_float32 val, count = 0;
	Numa* histoNorm = numaNormalizeHistogram(histo, 1);
	l_int32 median = 0;
	for (int i = 0; i < 256; i++) {
		numaGetFValue(histoNorm, i, &val);
		count += val;
		if (count >= 0.5) {
			median = i;
			break;
		}
	}
	numaDestroy(&histo);
	numaDestroy(&histoNorm);

	l_float32 lower_threshold = lower_percent * (max - min) + min;
	l_float32 upper_threshold = upper_percent * (max - min) + min;
//	l_float32 lower_threshold = median * 0.66;
//	l_float32 upper_threshold = median * 1;

	printf("lt=%f, ut=%f,median=%i,  max=%i, min=%i\n", lower_threshold, upper_threshold, median, max, min);
	//lower_threshold = 1;
	//upper_threshold = 25;
	/*
	 Hysteresis threshold.
	 */
	//TODO reuse as cache
	pixClearAll(orientation);
	l_int32* cache = new l_int32[nx * ny];
	Pix* result = pixCreate(nx, ny, 1);
	l_uint32 wplr = pixGetWpl(result);
	l_uint32 * datar = pixGetData(result);

	for (int i = 1; i < ny - 1; i++) {
		nms = datanms + i * wplnms;
		out = datar + i * wplr;
		for (int j = 1; j < nx - 1; j++) {
			if (GET_DATA_BYTE(nms,j) >= upper_threshold && GET_DATA_BIT(out,j) == 0) { // trace edges
				SET_DATA_BIT(out, j);
				int nedges = 1;
				cache[0] = i * nx + j;

				do {
					nedges--;
					l_int32 t = cache[nedges];
					l_int32 nbs[8]; // neighbours
					nbs[0] = t - nx;     // nn
					nbs[4] = nbs[0] + 1; // nw
					nbs[5] = nbs[0] - 1; // ne
					nbs[1] = t + nx;     // ss
					nbs[2] = t + 1;      // ww
					nbs[3] = t - 1;      // ee
					nbs[6] = nbs[1] + 1; // sw
					nbs[7] = nbs[1] - 1; // se
					for (int k = 0; k < 8; k++) {
						l_uint32 x, y;
						x = nbs[k] % nx;
						y = nbs[k] / nx;
						if (nbs[k] >= 0 && x < nx) {
							l_uint32 line = y * wplnms;
							l_uint32* liner = datar + y * wplr;
							l_uint8 val = GET_DATA_BYTE(datanms + line, x);
							if (val >= lower_threshold && GET_DATA_BIT(liner,x) == 0) {
								SET_DATA_BIT(liner, x);
								cache[nedges] = nbs[k];
								nedges++;
							}
						}
					}
				} while (nedges > 0);
			}
		}
	}
	if(debug){
		pixWrite("canny_result.png", result, IFF_PNG);
	}
	printf("canny hysteresis=%f\n", stopTimerNested(timer));

	pixDestroy(&edges);
	pixDestroy(&pixNms);
	pixDestroy(&orientation);
	delete[] cache;
	return result;
}
