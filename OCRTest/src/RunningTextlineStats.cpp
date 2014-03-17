#include "RunningTextlineStats.h"
#include <cmath>
#include <vector>

RunningTextlineStats::RunningTextlineStats() {
}

void RunningTextlineStats::Push(double x) {
	stats.Push(x);
}

int RunningTextlineStats::Count() const{
	return stats.NumDataValues();
}

void RunningTextlineStats::Clear() {
	stats.Clear();
}

double RunningTextlineStats::Mean() const {
	return stats.Mean();
}

double RunningTextlineStats::Variance() const {
	return stats.Variance();
}

double RunningTextlineStats::StandardDeviation() const {
	return stats.StandardDeviation();
}

bool RunningTextlineStats::Fits(double lineHeight) const {
	if (stats.NumDataValues() == 0) {
		return true;
	}
	RunningStats nextStats;
	nextStats+=stats;
	nextStats.Push(lineHeight);

	double mean = stats.Mean();
	double nextMean = nextStats.Mean();
	double maxMean = mean * 1.2;
	double minMean = mean * 0.8;
	double stddev = (stats.NumDataValues() == 1) ? 0 : stats.StandardDeviation();
	double maxStddev = stddev + 0.5;
	double nextStddev = nextStats.StandardDeviation();
	if (stddev == 0) {
		//use mean to decide whether the next line belongs to current group
		if (nextMean < minMean || nextMean > maxMean) {
			return false;
		}
	} else {
		//use std dev
		if (nextStddev > maxStddev) {
			return false;
		}
	}
	return true;
}

