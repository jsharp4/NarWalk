server = tcpip('0.0.0.0', 3000, 'NetWorkRole', 'server')
fopen(server)

i = 1
data = zeros(100, 1)
while 1
    if server.BytesAvailable > 0
        data(i) = fread(server, 1, 'float32')
        i = i + 1
        if i > 100
            break
        end
    end
end

plot(data)