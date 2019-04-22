server = tcpip('0.0.0.0', 3000, 'NetWorkRole', 'server')
fopen(server)

i = 1
while 1
    if server.BytesAvailable > 0
        data = fread(server, 1, 'double');
        signal(i) = data;
    end
    i = i + 1;
    if (i > 441)
        break
    end
end

server = tcpip('0.0.0.0', 3000, 'NetWorkRole', 'server')
fopen(server)

i = 1
even = 1
odd = 1
while 1
    if server.BytesAvailable > 0
        data = fread(server, 1, 'int16');
        if (mod(i - 1, 4) == 0 | mod(i - 1, 4) == 1)
            signal_even(even) = data;
            even = even + 1;
        else
            signal_odd(odd) = data;
            odd = odd + 1;
        end
        i = i + 1;
    end
    if (i > 22050)
        break
    end
end

subplot(3, 1, 1)
plot(signal_even)
subplot(3, 1, 2)
plot(signal_odd)

even_size = size(signal_even)
odd_size = size(signal_odd)
if even_size(1) > odd_size(1)
    min_size = odd_size(1)
else
    min_size = even_size(1)
end

diff = signal_even(1:min_size) - signal_odd(1:min_size)
subplot(3, 1, 3)
plot(diff)