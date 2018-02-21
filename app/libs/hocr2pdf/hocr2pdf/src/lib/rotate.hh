/*
 * Copyright (C) 2006 - 2010 Rene Rebe, ExactCODE GmbH Germany.
 *           (C) 2006, 2007 Archivista GmbH, CH-8042 Zuerich
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

#include "Image.hh"

void flipX (Image& image);
void flipY (Image& image);

void rotate (Image& image, double angle, const Image::iterator& background);

void exif_rotate(Image& image, unsigned exif_orientation);

Image* copy_crop_rotate (Image& image, int x_start, int y_start,
			 unsigned int w, unsigned int h,
			 double angle, const Image::iterator& background);
