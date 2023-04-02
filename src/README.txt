If not at src folder, use the command 'cd src'
After that, compile all java files by using commands:
    javac *.java
    cd util
    javac *.java

To use the program, start the receiver first by using command 'java Receiver 8000 4'. You are free to use any other number instead of 8000 for port and any other positive number for window size.
To start the Sender, use command 'java Sender <hostname> <port number> <window size>'. You have to determine the hostname of Receiver and the port number that the Receiver program is running. Also specify a positive window size.