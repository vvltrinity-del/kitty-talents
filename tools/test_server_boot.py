"""Dedicated-server boot check: start runServer (run-server/), wait for "Done (", send "stop", and make sure the
whole process tree is gone afterwards.

Exit code 0 = the server reached "Done (" (booted without e.g. client-class NoClassDefFoundError); 1 otherwise.

Two pitfalls this script handles:
  * after 'stop', the server's output must keep being drained, or a full stdout pipe blocks it mid-shutdown;
  * on Windows, Popen.kill() only ends gradlew.bat; the Gradle daemon and server JVM would survive as orphans
    holding the world lock and the server port. kill_tree() ends the whole tree.
"""
import os
import subprocess
import sys
import threading
import time

BOOT_TIMEOUT_S = 150
SHUTDOWN_TIMEOUT_S = 45


def kill_tree(proc):
    if proc.poll() is not None:
        return
    if os.name == "nt":
        subprocess.run(["taskkill", "/T", "/F", "/PID", str(proc.pid)], capture_output=True)
    else:
        proc.kill()


def main():
    print("=== Starting Dedicated Server Boot Verification ===")
    gradlew = os.path.abspath("gradlew.bat" if os.name == "nt" else "gradlew")
    proc = subprocess.Popen([gradlew, "runServer", "--no-daemon"], stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, bufsize=1, cwd=os.getcwd())
    start = time.time()
    booted = False
    try:
        for line in iter(proc.stdout.readline, ''):
            print(line, end='', flush=True)
            if "Done (" in line:
                booted = True
                print("\n>>> SERVER REACHED 'Done ('! Issuing 'stop' command... <<<\n", flush=True)
                try:
                    proc.stdin.write("stop\n")
                    proc.stdin.flush()
                except Exception as e:  # noqa: BLE001 - report and fall through to the kill
                    print(f"Error sending stop: {e}")
                break
            if time.time() - start > BOOT_TIMEOUT_S:
                print(f"\n>>> TIMEOUT REACHED ({BOOT_TIMEOUT_S}s) <<<\n", flush=True)
                break

        drain = threading.Thread(target=lambda: [print(l, end='', flush=True) for l in iter(proc.stdout.readline, '')],
                                 daemon=True)
        drain.start()
        try:
            proc.wait(timeout=SHUTDOWN_TIMEOUT_S)
        except subprocess.TimeoutExpired:
            print(f">>> Process did not stop in {SHUTDOWN_TIMEOUT_S}s, killing the process tree... <<<")
    finally:
        kill_tree(proc)

    print(f"\n=== Server Execution Completed in {time.time() - start:.1f}s ===")
    print(f"Server Booted (Done detected): {booted}")
    print(f"Process Exit Code: {proc.returncode}")
    sys.exit(0 if booted else 1)


if __name__ == "__main__":
    main()
