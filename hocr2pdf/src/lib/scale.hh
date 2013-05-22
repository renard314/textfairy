// pick the best
void scale (Image& image, double xscale, double yscale);

// explicit versions
void nearest_scale (Image& image, double xscale, double yscale);
void box_scale (Image& image, double xscale, double yscale);

void bilinear_scale (Image& image, double xscale, double yscale);
void bicubic_scale (Image& image, double xscale, double yscale);

void ddt_scale (Image& image, double xscale, double yscale);

void thumbnail_scale (Image& image, double xscale, double yscale);
