#!/usr/bin/env python3
import os
import json
import socket
import subprocess
import sys

def get_lan_ip() -> str:
    """Bulletproof method to retrieve the active LAN IP of this machine."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Doesn't need to be reachable, just triggers local routing table lookup
        s.connect(('10.255.255.255', 1))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

def update_json_config(ip: str) -> bool:
    """Updates the network_config.json file with the new IP. Returns True if changed."""
    config_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "network_config.json")
    
    config_data = {}
    if os.path.exists(config_path):
        try:
            with open(config_path, "r", encoding="utf-8") as f:
                config_data = json.load(f)
        except Exception as e:
            print(f"[-] Warning: Failed to parse existing network_config.json: {e}")
            
    old_ip = config_data.get("LOCAL_BACKEND_IP")
    if old_ip == ip:
        print(f"[+] network_config.json is already up to date with IP: {ip}")
        return False
        
    config_data["LOCAL_BACKEND_IP"] = ip
    try:
        with open(config_path, "w", encoding="utf-8") as f:
            json.dump(config_data, f, indent=2)
        print(f"[+] Successfully updated network_config.json: {old_ip} -> {ip}")
        return True
    except Exception as e:
        print(f"[-] Error writing network_config.json: {e}")
        sys.exit(1)

def commit_and_push_changes(ip: str):
    """Stages, commits and pushes the updated network_config.json to GitHub."""
    print("[*] Staging network_config.json changes...")
    try:
        subprocess.run(["git", "add", "network_config.json"], check=True)
        
        commit_msg = f"config: auto-update local backend IP to {ip}"
        print(f"[*] Committing changes: '{commit_msg}'...")
        subprocess.run(["git", "commit", "-m", commit_msg], check=True)
        
        print("[*] Pushing changes to remote repository...")
        subprocess.run(["git", "push", "origin", "main"], check=True)
        
        print("[+] Sincronizzazione completata! Repository pulito.")
    except subprocess.CalledProcessError as e:
        print(f"[-] Git command failed: {e}")
        print("[-] Please ensure git is configured and has remote push access.")
        sys.exit(1)

def main():
    print("==========================================================")
    print("      SmartGrocery Manager IP Auto-Update & Sync Tool     ")
    print("==========================================================")
    
    lan_ip = get_lan_ip()
    print(f"[+] Detected active LAN IP: {lan_ip}")
    
    changed = update_json_config(lan_ip)
    if changed:
        commit_and_push_changes(lan_ip)
    else:
        print("[+] No changes detected. Repository remains clean.")

if __name__ == "__main__":
    main()
