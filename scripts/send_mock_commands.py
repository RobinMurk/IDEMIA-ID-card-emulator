#!/usr/bin/env python3
import sys
from smartcard.CardType import AnyCardType
from smartcard.CardRequest import CardRequest
from smartcard.CardConnection import CardConnection
from smartcard.util import toHexString
from datetime import datetime



def send(apdu, channel) -> bytes:
    print("sending APDU: ", apdu)
    data, sw1, sw2 = channel.transmit(apdu)
    print("data recived...")
    print(f'response: {data}, {hex(sw1)} ,{hex(sw2)}')
    # success
    if [sw1,sw2] == [0x90,0x00]:
        return data
    # signals that there is more data to read
    elif sw1 == 0x6C:
        print("[=] Resending with Le:", sw2)
        return send(apdu[0:4] + [sw2]) # resend APDU with Le = sw2
    else:
        print(f"Error: {sw1} {sw2}, sending APDU: {toHexString(apdu)}")
        exit(1)


def getLog(apdu, channel) -> bytes:
    print("sending APDU: ", apdu)
    data, sw1, sw2 = channel.transmit(apdu)
    print("data recived...")
    print(data, hex(sw1) ,hex(sw2))
    print(f'length of incoming data: {len(data)}')
    # success
    if [sw1,sw2] == [0x90,0x00]:
        return data
    elif sw1 == 0x6C:
        print("[=] Resending with Le:", sw2)
        return data + getLog(apdu[0:4] + [sw2], channel)
    else:
        print(f"Error: {sw1} {sw2}, sending APDU: {toHexString(apdu)}")
        exit(1)



def execute_command():
    try:

        
        channel = CardRequest(timeout=100, cardType=AnyCardType()).waitforcard().connection
        print("[+] Selected reader:", channel.getReader())

        try:
            channel.connect(CardConnection.T0_protocol)
            print("[+] T=0 selected")
        except:
            channel.connect(CardConnection.T1_protocol)
            print("[+] T=1 selected")

        #not needed as applet is default    
        #SELECT APPLET
        #send([0x00,0xA4,0x04,0x00,0x10] + [0xA0, 0x00, 0x00, 0x00, 0x77,0x01,0x08,0x00,0x07,0x00,0x00,0xFE,0x00,0x00,0x01,0x00], channel)

        #CHANGE ATR HISTORIC BYTES
        if P1 == 0x01:
            atr = channel.getATR()
            print("current ATR: ",toHexString(atr))
            send([0x00,0xDD,P1,P2,0x02], channel)
            print("setATRhistbytes() was successful")

        elif P1 == 0x02:

            #GET LOGGER DATA
            if P2 == 0x01:
                print("getting logger data...")
                data = getLog([0x00,0xDD,P1,P2,0x00], channel)
                print('--------------------------------')
                print("data size: ", len(data))

                pretty_print = ""
                file_name = f'logs/APDU_log_{log_name}_{datetime.now()}'
                #prints out to console
                print(data)
                i = 0
                while i < len(data):
                    len_data = data[i]
                    i += 1
                    bytes_recived = data[i:i+len_data]
                    i += len_data
                    if len(bytes_recived) > 1:
                        if bytes_recived[1] == 0xA4:
                            text = "SELECT FILE: AID" if bytes_recived[2] == 0x04 else "SELECT FILE"
                            pretty_print += f"{toHexString(bytes_recived)} {text}\n"
                        elif bytes_recived[1] == 0xB0:
                            pretty_print += f"{toHexString(bytes_recived)} READ BINARY\n"
                        else:
                            pretty_print += toHexString(bytes_recived) + '\n'
                    elif len(bytes_recived) == 1:
                        match bytes_recived[0]:
                            case 0x00:
                                pretty_print += "T=0\n"
                            case 0x01:
                                pretty_print += "T=1\n"
                            case 0x91 | 0x81:
                                pretty_print += "T=CL\n"
                            case _:
                                pretty_print += f"Unknown: {toHexString(bytes_recived)}\n"
                print(pretty_print)
                
                #saves to file
                if save_flag:
                    print("-------------------------")
                    with open (file_name, 'w') as f:
                        i = 0
                        while i < len(data):
                            len_data = data[i]
                            i += 1
                            bytes_recived = data[i:i+len_data]
                            i += len_data
                            if len(bytes_recived) > 1:
                                if bytes_recived[1] == 0xA4:
                                    text = "SELECT FILE: AID" if bytes_recived[2] == 0x04 else "SELECT FILE"
                                    
                                    f.write(f"{toHexString(bytes_recived)} {text}\n")
                                elif bytes_recived[1] == 0xB0:
                                    f.write(f"{toHexString(bytes_recived)} READ BINARY\n")
                                else:
                                    f.write(f"{toHexString(bytes_recived)}\n")
                            elif len(bytes_recived) == 1:
                                match bytes_recived[0]:
                                    case 0x00:
                                        f.write("T=0\n")
                                    case 0x01:
                                        f.write("T=1\n")
                                    case 0x91 |0x81:  #feitian card and G & D card specific, might vary for other cards
                                        f.write("T=CL\n")
                                    case _:
                                        f.write(f"Unknown: {toHexString(bytes_recived)}\n")

            #RESET LOGGER
            elif P2 == 0x02:
                print("reseting logger...")
                send([0x00,0xDD,P1,P2,0x02], channel)
                print("Logger reset")

            #GET LOGGER INDEX
            elif P2 == 0x03:
                print("getting logger index...")
                data = send([0x00,0xDD,P1,P2,0x02], channel)
                print("Logger index: ", int.from_bytes(data, byteorder='big'))

        elif P1 == 0x00:
            print("no command set")
    except:
        print("[-] system error")
        exit(1)


P1 = 0x00
P2 = 0x00
log_name = ""
save_flag = False

help_text = """ 
Send mock commands to the IDApplet ID card emulator\n
NB: Commands are tested to work over T=0 protocol only\n
Usage: python3 send_mock_commands.py [options]\n
Options:\n
--help => prints this help text\n
--feitian_card => sets the Feitian card's original historic bytes\n
--id_card => sets the card's historic bytes to the 2018 IDEMIA ID card's historic bytes\n
--gd_card => sets the G&D card's original historic bytes\n
--logger => retrives the loggers data\n
\t--save => saves the loggers data to a file. Must be present with --logger flag\n
\t--name => name of the file. Must be present with --logger flag\n
--reset-logger => resets the logger\n
--logger-index => retrives the loggers index
            """


if sys.argv.__contains__("--name"):
    index = sys.argv.index("--name")
    if index + 1 < len(sys.argv):
        log_name = sys.argv[index + 1]

if sys.argv.__contains__("--save"):
    save_flag = True

if len(sys.argv) >= 2:
    match sys.argv[1]:
        case "--feitian_card":
            P1 = 0x01
            P2 = 0x02
            execute_command() 
        case "--id_card":
            P1 = 0x01
            P2 = 0x01
            execute_command()
        case "--gd_card":
            P1 = 0x01
            P2 = 0x03
            execute_command()
        case "--logger":
            P1 = 0x02
            P2 = 0x01
            execute_command()
        case "--reset-logger":
            P1 = 0x02
            P2 = 0x02
            execute_command()
        case "--logger-index":
            P1 = 0x02
            P2 = 0x03
            execute_command()
        case "--help":
            print(help_text)
        case _:
            print(help_text)
        


        
        

    