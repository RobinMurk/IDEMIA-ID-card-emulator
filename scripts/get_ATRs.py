#!/usr/bin/python


from smartcard.CardType import AnyCardType
from smartcard.CardRequest import CardRequest
from smartcard.CardConnection import CardConnection
from smartcard.util import toHexString
from smartcard import scard

channel:CardConnection = CardRequest(timeout=100, cardType=AnyCardType()).waitforcard().connection

protocol = CardConnection.T0_protocol
protocol_fallback = CardConnection.T1_protocol

protocol = 0
try:
    protocol = CardConnection.T1_protocol
    channel.connect(protocol)
    print("[+] T=1 selected")
except:
    protocol = CardConnection.T0_protocol
    channel.connect(protocol)
    print("[+] T=0 selected")


channel.connect(protocol, disposition=scard.SCARD_UNPOWER_CARD)
channel.disconnect()

channel.connect(protocol, disposition=scard.SCARD_RESET_CARD)
r = channel.getATR()
print(f"Cold ATR({len(bytes(r))}): {toHexString(r)}")
channel.disconnect()

channel.connect(protocol, disposition=scard.SCARD_UNPOWER_CARD)
r = channel.getATR()
print(f"Warm ATR({len(bytes(r))}): {toHexString(r)}")
channel.disconnect()
