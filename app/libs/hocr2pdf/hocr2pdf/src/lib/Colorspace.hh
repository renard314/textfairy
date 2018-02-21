/*
 * Colorspace conversions.
 * Copyright (C) 2006 - 2010 René Rebe, ExactCOD GmbH Germany
 * Copyright (C) 2007 Susanne Klaus, ExactCODE
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2. A copy of the GNU General
 * Public License can be found in the file LICENSE.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANT-
 * ABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * Alternatively, commercial licensing options are available from the
 * copyright holder ExactCODE GmbH Germany.
 */

#ifndef COLORSPACE_HH
#define COLORSPACE_HH

#include "Image.hh"

void normalize (Image& image, unsigned char low = 0, unsigned char high = 0);

void colorspace_rgb8_to_gray8 (Image& image, const int bytes = 3);
void colorspace_rgb8_to_rgb8a (Image& image, uint8_t alpha=0xff);

void colorspace_gray8_threshold (Image& image, uint8_t threshold = 127);
void colorspace_gray8_denoise_neighbours (Image &image, bool gross = false);

void colorspace_gray8_to_gray1 (Image& image, uint8_t threshold = 127);
void colorspace_gray8_to_gray2 (Image& image);
void colorspace_gray8_to_gray4 (Image& image);

void colorspace_gray1_to_gray2 (Image& image);
void colorspace_gray1_to_gray4 (Image& image);

void colorspace_grayX_to_gray8 (Image& image);
void colorspace_gray8_to_rgb8 (Image& image);
void colorspace_grayX_to_rgb8 (Image& image);

void colorspace_16_to_8 (Image& image);
void colorspace_8_to_16 (Image& image);

// the threshold is used during conversion to b/w formats
bool colorspace_convert (Image& image, int spp, int bps, uint8_t threshold = 127);
bool colorspace_by_name (Image& image, const std::string& target_colorspace,
			 uint8_t threshold = 127);

//const char* colorspace_name (Image& image);

void brightness_contrast_gamma (Image& image, double b, double c, double g);
void hue_saturation_lightness (Image& image, double h, double s, double v);

void invert (Image& image);

// "internal" helper (for image loading)

void colorspace_de_palette (Image& image, int table_entries,
			    uint16_t* rmap, uint16_t* gmap, uint16_t* bmap);

#endif
