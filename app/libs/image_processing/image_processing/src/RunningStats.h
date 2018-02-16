#ifndef RUNNINGSTATS_H
#define RUNNINGSTATS_H

class RunningStats
{
public:
    RunningStats();
    void Clear();
    void Push(double x);
    long long NumDataValues() const;
    double Mean() const;
    double Variance() const;
    double PopulationVariance() const;
    double StandardDeviation() const;
    double PopulationStandardDeviation() const;
    double Skewness() const;
    double Kurtosis() const;

    friend RunningStats operator+(const RunningStats a, const RunningStats b);
    RunningStats& operator+=(const RunningStats &rhs);

private:
    long long n;
    double M1, M2, M3, M4;
};

#endif
