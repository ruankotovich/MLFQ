#!/bin/sh

echo "Executing IOBOUND test in parallel..."
./xperform.sh 1 2 1 5 ioBound >> ioB1.txt &
./xperform.sh 1 2 6 10 ioBound >> ioB2.txt &
./xperform.sh 3 4 1 5 ioBound >> ioB3.txt &
./xperform.sh 3 4 5 10 ioBound >> ioB4.txt
echo "Done."
cat ioB?.txt > ioTest.txt
rm ioB?.txt


echo "Executing HALFBOUND test in parallel..."
./xperform.sh 1 2 1 5 halfBound >> hbB1.txt &
./xperform.sh 1 2 6 10 halfBound >> hbB2.txt &
./xperform.sh 3 4 1 5 halfBound >> hbB3.txt &
./xperform.sh 3 4 6 10 halfBound >> hbB4.txt

cat hbB?.txt > hbTest.txt
rm rbB?.txt
echo "Done."

echo "Executing CPUBOUND test in parallel..."
./xperform.sh 1 2 1 5 cpuBound >> cpuB1.txt &
./xperform.sh 1 2 6 10 cpuBound >> cpuB2.txt &
./xperform.sh 3 4 1 5 cpuBound >> cpuB3.txt &
./xperform.sh 3 4 6 10 cpuBound >> cpuB4.txt

cat cpuB?.txt > cpuTest.txt
rm cpuB?.txt
echo "Done."
