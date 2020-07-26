# Reliable-data-transfer-at-application-layer

## Overview
This code implements different protocols for achieving end-to-end reliable data transfer  at the application layer over the unreliable datagram protocol (UDP) transport protocol.  In particular, it implements three different sliding window protocols – Stop-and-Wait, Go Back N and Selective Repeat – at the application layer using UDP sockets in Java. For each  of the three sliding window protocols, two protocol endpoints referred henceforth as sender and receiver are implemented; these  endpoints also act as application programs. Data communication is unidirectional, requiring transfer of a large file from the sender to the  receiver over a link as a sequence of smaller messages. The underlying link is assumed to be symmetric in terms of bandwidth and delay characteristics.

### Part 1a
Basic framework (large file transmission under ideal conditions)

### Part 1b
Stop-and-Wait

### Part 2a
Go-Back-N

### Part 2b
Selective Repeat
