/*  This file is part of Text Fairy.
 
 Text Fairy is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 Text Fairy is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with Text Fairy.  If not, see <http://www.gnu.org/licenses/>.
 */
#include "dewarp_textfairy.h"
#include <stdio.h>

/*!
 *  pixDewarp()
 *
 *      Input:  pixb binary image to be modified
 *              &pixd (<return> disparity corrected image)
 *      Return: 0 if OK, 1 on error
 */
l_int32 pixDewarp(Pix* pixb, Pix** pixd) {
	L_DEWARP   *dew;
	L_DEWARPA  *dewa;
	l_int32 vsuccess, applyResult = 1;

	dewa = dewarpaCreate(0, 15, 1, 8, 0);
	//relax constraints on max curves as pictures taken by phone cameras can be extremely distorted
	dewarpaSetCurvatures(dewa, -1, 0, 330, -1, -1, -1);
	dewarpaUseBothArrays(dewa, 1);  // try to use both disparity arrays for this example
	// Initialize a Dewarp for this page (say, page 214)
	dew = dewarpCreate(pixb, 1);
	// Insert in Dewarpa and obtain parameters for building the model
	dewarpaInsertDewarp(dewa, dew);
	// Do the work
	dewarpBuildPageModel(dew,NULL);  // no debugging
	dewarpaModelStatus(dewa, 1, &vsuccess, NULL);
	if (vsuccess) {
		applyResult = dewarpaApplyDisparity(dewa,1, pixb, 255,0,0,pixd, NULL);
	}
	dewarpaDestroy(&dewa);
	return applyResult;
}

