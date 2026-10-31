# 1. Stop the bleeding if a rollout caused it
kubectl rollout undo deployment/orders
kubectl rollout status deployment/orders

# 2. What state are the pods in, and why?
kubectl get pods -l app=orders -o wide
# Events at the bottom: probe failures, OOMKilled, image pulls, scheduling
kubectl describe pod orders-7c9d…
kubectl get events --sort-by=.lastTimestamp | tail -20

# 3. Logs — including the previous (crashed) container
kubectl logs orders-7c9d… --previous
kubectl logs -l app=orders --since=10m --all-containers

# 4. Was it the memory limit? (exit code 137, reason OOMKilled)
kubectl get pod orders-7c9d… -o jsonpath='{.status.containerStatuses[0].lastState.terminated}'
kubectl top pod -l app=orders                # live CPU/memory (metrics-server)

# 5. Look inside a running pod
kubectl exec -it orders-7c9d… -- sh
# needs -XX:NativeMemoryTracking=summary
kubectl exec orders-7c9d… -- jcmd 1 VM.native_memory summary
# ephemeral debug container (distroless images)
kubectl debug -it orders-7c9d… --image=busybox --target=orders

# 6. Is the Service routing to anything?
kubectl get endpointslices -l kubernetes.io/service-name=orders
kubectl port-forward svc/orders 8080:80      # then curl localhost:8080/actuator/health
