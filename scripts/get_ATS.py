#!/usr/bin/env python3

import datetime, sys

from smartcard.CardType import AnyCardType
from smartcard.CardRequest import CardRequest
from smartcard.CardConnection import CardConnection
from smartcard.util import toHexString
from smartcard import scard

channel:CardConnection = CardRequest(timeout=100, cardType=AnyCardType()).waitforcard().connection
print("[+] Selected reader:", channel.getReader())


protocol = CardConnection.T1_protocol
channel.connect(protocol, disposition=scard.SCARD_UNPOWER_CARD)
channel.disconnect()
channel.connect(protocol, disposition=scard.SCARD_RESET_CARD)

def send(apdu):
    data, sw1, sw2 = channel.transmit(apdu)

    if [sw1,sw2] == [0x90,0x00]:
        return data
    elif sw1 == 0x61:
        #print("[=] More data to read:", sw2)
        return send([0x00, 0xC0, 0x00, 0x00, sw2]) # GET RESPONSE of sw2 bytes
    elif sw1 == 0x6C:
        #print("[=] Resending with Le:", sw2)
        return send(apdu[0:4] + [sw2]) # resend APDU with Le = sw2
    else:
        print(f"Error: {sw1} {sw2}, sending APDU: {toHexString(apdu)}")
        sys.exit(1)


atr = channel.getATR()
if atr == [0x3B,0xDB,0x96,0x00,0x80,0xB1,0xFE,0x45,0x1F,0x83,0x00,0x12,0x23,0x3F,0x53,0x65,0x49,0x44,0x0F,0x90,0x00,0xF1]:
    print("[+] Estonian ID card (2018)")
elif atr == [0x3B,0x6A,0x00,0x00,0x09,0x44,0x31,0x31,0x43,0x52,0x02,0x00,0x25,0xC3]:
    print("[+] Feitian FT-Java/D11CR")
elif atr == [0x3B, 0xFE, 0x18, 0x00, 0x00, 0x80, 0x31, 0xFE, 0x45, 0x53, 0x43, 0x45, 0x36, 0x30, 0x2D, 0x43, 0x44, 0x30, 0x38, 0x31, 0x2D, 0x6E, 0x46, 0xA9]:
    print("[+] G&D SmartCafe Expert 6.0 80K Dual Card")
else:
    print("[-] Unknown card:", toHexString(atr))

def now():
    return datetime.datetime.now()

def timediff(s):
    return (datetime.datetime.now()-s).total_seconds()



r = send([0xFF,0xCA,0x01,0x00,0x00])
ats = bytes(r)
print("ATS (%s): %s" % (len(ats), (toHexString(r))))

r = send([0xFF,0xCA,0x00,0x00,0x00])
ats = bytes(r)
print("UID (%s): %s" % (len(ats), toHexString(r)))
