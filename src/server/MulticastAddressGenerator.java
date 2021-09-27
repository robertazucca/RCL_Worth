package server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MulticastAddressGenerator {

    private static final int a1 = 239;
    private static int a2 = 0;
    private static int a3 = 0;
    private static int a4 = 0;
    private static final int max_a1 = 239;
    private static final int max_a2 = 256;
    private static final int max_a3 = 256;
    private static final int max_a4 = 256;
    private static final String max_address = "239.255.255.255";
    private static final List<String> listaReuse = new ArrayList<>();


    public static synchronized String getAddress()
    {
        if (!listaReuse.isEmpty()) {
            return listaReuse.remove(0);
        }
        String address = a1 + "." + a2 + "." + a3 + "." + a4;

        if (address.equals(max_address)) {
            throw new IllegalArgumentException("Esauriti indirizzi disponibili");
        }

        a4++;
        a4 %= max_a4;
        if (a4 == 0) {
            a3++;
            a3 %= max_a3;
            if (a3 == 0) {
                a2++;
            }
        }

        return address;

    }

    public static synchronized void freeAddress(String address) throws IOException {

        if(InetAddress.getByName(address).isMulticastAddress())
            listaReuse.add(address);
    }


}
