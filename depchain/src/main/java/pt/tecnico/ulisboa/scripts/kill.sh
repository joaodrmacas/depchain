for PORT in 8079 8080 9090 8081 9091 8082 9092 8083 9093 8084 9094 10009 10011; do
    PID=$(lsof -t -i :$PORT)
    if [ -n "$PID" ]; then
        kill -9 "$PID"
        echo "Killed process on port $PORT"
    else
        echo "No process found on port $PORT"
    fi
done