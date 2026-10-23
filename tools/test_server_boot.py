import subprocess
import sys
import time
import os

print("=== Starting Dedicated Server Boot Verification ===")
gradlew = os.path.abspath("gradlew.bat")
cmd = [gradlew, "runServer", "--no-daemon"]

start_time = time.time()
proc = subprocess.Popen(
    cmd,
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    bufsize=1,
    cwd=os.getcwd()
)

server_booted = False
clean_shutdown = False

try:
    for line in iter(proc.stdout.readline, ''):
        print(line, end='', flush=True)
        if "Done (" in line:
            server_booted = True
            print("\n>>> SERVER REACHED 'Done ('! Issuing 'stop' command... <<<\n", flush=True)
            try:
                proc.stdin.write("stop\n")
                proc.stdin.flush()
            except Exception as e:
                print(f"Error sending stop: {e}")
            break
        if time.time() - start_time > 150:
            print("\n>>> TIMEOUT REACHED (150s) <<<\n", flush=True)
            break

    # Wait for graceful termination after stop
    try:
        proc.wait(timeout=30)
        clean_shutdown = (proc.returncode == 0)
    except subprocess.TimeoutExpired:
        print(">>> Process did not stop in 30s, terminating... <<<")
        proc.kill()

except Exception as ex:
    print(f"Exception during server execution: {ex}")
    proc.kill()

print(f"\n=== Server Execution Completed in {time.time() - start_time:.1f}s ===")
print(f"Server Booted (Done detected): {server_booted}")
print(f"Process Exit Code: {proc.returncode}")

if not server_booted:
    sys.exit(1)
sys.exit(0)
