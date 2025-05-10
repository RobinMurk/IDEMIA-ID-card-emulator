#!/usr/bin/env python3

from smartcard.CardType import AnyCardType
from smartcard.CardRequest import CardRequest
from smartcard.CardConnection import CardConnection
from smartcard.util import toHexString


channel = CardRequest(timeout=100, cardType=AnyCardType()).waitforcard().connection
print("[+] Selected reader:", channel.getReader())

try:
    channel.connect(CardConnection.T0_protocol)
    print("[+] T=0 selected")
except:
    channel.connect(CardConnection.T1_protocol)
    print("[+] T=1 selected")

atr = channel.getATR()

if atr == [0x3B,0xDB,0x96,0x00,0x80,0xB1,0xFE,0x45,0x1F,0x83,0x00,0x12,0x23,0x3F,0x53,0x65,0x49,0x44,0x0F,0x90,0x00,0xF1]:
    print("[+] Estonian ID card (2018)")
elif atr == [0x3B,0x6A,0x00,0x00,0x09,0x44,0x31,0x31,0x43,0x52,0x02,0x00,0x25,0xC3]:
    print("[+] Feitian FT-Java/D11CR")
elif atr == [0x3B, 0xFE, 0x18, 0x00, 0x00, 0x80, 0x31, 0xFE, 0x45, 0x53, 0x43, 0x45, 0x36, 0x30, 0x2D, 0x43, 0x44, 0x30, 0x38, 0x31, 0x2D, 0x6E, 0x46, 0xA9]:
    print("[+] G&D SmartCafe Expert 6.0 80K Dual Card")
else:
    print("[-] Unknown card:", toHexString(atr))

print("ATR: ",toHexString(atr))


def send(apdu):
    data, sw1, sw2 = channel.transmit(apdu)
    # success
    if [sw1,sw2] == [0x90,0x00]:
        return data
    # signals that there is more data to read
    elif sw1 == 0x61:
        print("[=] More data to read:", sw2)
        return send([0x00, 0xC0, 0x00, 0x00, sw2]) # GET RESPONSE of sw2 bytes
    elif sw1 == 0x6C:
        print("[=] Resending with Le:", sw2)
        return send(apdu[0:4] + [sw2]) # resend APDU with Le = sw2
    else:
        print(f"Error: {sw1} {sw2}, sending APDU: {toHexString(apdu)}")
        exit(1)
        
    
results = dict()
file_ptr = 0
try:
    # reading personal data file
    #SELECT APPLET
    send([0x00,0xA4,0x04,0x00,0x10] + [0xA0, 0x00, 0x00, 0x00, 0x77,0x01,0x08,0x00,0x07,0x00,0x00,0xFE,0x00,0x00,0x01,0x00])
    #SELECT MASTER FILE
    send([0x00,0xA4,0x00,0x0C])
    #SELECT MF/5000
    send([0x00,0xA4,0x01,0x0C,0x02] + [0x50,0x00])
    print("--------------------------")
    print("[+] testing random pulling")
    #SELECT file 5006
    send([0x00,0xA4,0x01,0x0C,0x02] + [0x50,6])
    print("RESULT: ", bytes(send([0x00,0xB0,0x00,0x00,0x00])).decode("utf-8"))

    #SELECT file 5009
    send([0x00,0xA4,0x01,0x0C,0x02] + [0x50,9])
    print("RESULT: ", bytes(send([0x00,0xB0,0x00,0x00,0x00])).decode("utf-8"))
    print("--------------------------")

    #getting all the info from MF/5000/
    for i in range(1,16):
        #SELECT file 5000 + i
        send([0x00,0xA4,0x01,0x0C,0x02] + [0x50,i])
        #Read results
        results[i] = bytes(send([0x00,0xB0,0x00,0x00,0x00])).decode("utf-8")
except:
    print("[-] system error reading personal files")
    exit(1)

table_2018 = {
1:'Surname',
2:'First name',
3:'Sex',
4:'Citizenship',
5:'Date & place of birth',
6:'Personal ID code',
7:'Document number',
8:'Expiry date',
9:'Date & place of issuance',
10:'Type of residence permit',
11:'Notes line 1',
12:'Notes line 2',
13:'Notes line 3',
14:'Notes line 4',
15:'Notes line 5',
}

# print all enteries from the personal data file
print("[+] Personal data file:")
for i in range(1, 16):
    print(table_2018[i] + ": " + results[i])

