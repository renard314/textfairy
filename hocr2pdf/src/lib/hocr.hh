/*
 * The ExactImage library's hOCR to PDF parser
 * Copyright (C) 2008-2009 René Rebe, ExactCODE GmbH Germany
 * Copyright (C) 2008 Archivista
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

bool hocr2pdf(std::istream& hocrStream, PDFCodec* pdfContext,
	      unsigned int res,  bool sloppy = false, bool straightenTextLines = false,
	      std::ostream* txtStream = 0);
