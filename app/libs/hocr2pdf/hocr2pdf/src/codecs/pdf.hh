/*
 * Copyright (c) 2008 Susanne Klaus <susanne@exactcode.de>
 * Copyright (c) 2008 Rene Rebe <rene@exactcode.de>
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

#include "Codecs.hh"

struct PDFContext; // fwd

class PDFCodec : public ImageCodec {
public:
  
  PDFCodec ()
    : context(0)
  {
    registerCodec ("pdf", this);
  };

  ~PDFCodec ();
  
  // freestanding
  PDFCodec (std::ostream* s);
  
  virtual std::string getID () { return "PDF"; };
  
  virtual int readImage (std::istream* stream, Image& image, const std::string& decompres);
  virtual bool writeImage (std::ostream* stream, Image& image,
			   int quality, const std::string& compress);

  // direct PDF stream creation, including vector objects and
  // multiple pages
  
  enum filling_rule_t
    {
      fill_non_zero = (1<<0),
      fill_even_odd = (1<<1),
      fill_none = 0xff
    };
  
  void beginPage(double, double);
  
  void moveTo(double x, double y);
  void addLineTo(double x, double y);
  void closePath();
  void addCurveTo(double x1, double y1, double x2, double y2,
		  double x3, double y3);
  void setFillColor(double r, double g, double b);
  void setLineWidth(double width);
  void setLineDash(double offset, const std::vector<double>& dashes);
  void setLineDash(double offset, const double* dashes, int n);
  void showPath(filling_rule_t fill = fill_none);

  /* Default font names, as per PDF Reference:
     Times-Roman, Times-Bold, Times-Italic, Times-BoldItalic,
     Helvetica, Helvetica-Bold, Helvetica-Oblique, Helvetica-BoldOblique,
     Courier, Courier-Bold, Courier-Oblique, Courier-BoldOblique,
     Symbol, ZapfDingbats */
  void beginText();
  void textTo(double x, double y);
  void showText(const std::string& font, const std::string& text,
		double height);
  void showImage(Image& image, double x, double y,
		 double width, double height, int quality = 80,
		 const std::string& compress = "");
  void endText();
  
private:
  PDFContext* context;
};
