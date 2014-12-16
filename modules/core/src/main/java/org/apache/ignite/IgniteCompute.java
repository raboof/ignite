/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.apache.ignite;

import org.apache.ignite.cluster.*;
import org.apache.ignite.compute.*;
import org.apache.ignite.lang.*;
import org.apache.ignite.marshaller.optimized.IgniteOptimizedMarshaller;
import org.apache.ignite.resources.*;
import org.apache.ignite.spi.failover.FailoverSpi;
import org.apache.ignite.spi.loadbalancing.LoadBalancingSpi;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Defines compute grid functionality for executing tasks and closures over nodes
 * in the {@link ClusterGroup}. Instance of {@code GridCompute} is obtained from grid projection
 * as follows:
 * <pre name="code" class="java">
 * GridCompute c = GridGain.grid().compute();
 * </pre>
 * The methods are grouped as follows:
 * <ul>
 * <li>{@code apply(...)} methods execute {@link IgniteClosure} jobs over nodes in the projection.</li>
 * <li>
 *     {@code call(...)} methods execute {@link Callable} jobs over nodes in the projection.
 *     Use {@link IgniteCallable} for better performance as it implements {@link Serializable}.
 * </li>
 * <li>
 *     {@code run(...)} methods execute {@link Runnable} jobs over nodes in the projection.
 *     Use {@link IgniteRunnable} for better performance as it implements {@link Serializable}.
 * </li>
 * <li>{@code broadcast(...)} methods broadcast jobs to all nodes in the projection.</li>
 * <li>{@code affinity(...)} methods colocate jobs with nodes on which a specified key is cached.</li>
 * </ul>
 * Note that if attempt is made to execute a computation over an empty projection (i.e. projection that does
 * not have any alive nodes), then {@link ClusterGroupEmptyException} will be thrown out of result future.
 * <h1 class="header">Serializable</h1>
 * Also note that {@link Runnable} and {@link Callable} implementations must support serialization as required
 * by the configured marshaller. For example, {@link IgniteOptimizedMarshaller} requires {@link Serializable}
 * objects by default, but can be configured not to. Generally speaking objects that implement {@link Serializable}
 * or {@link Externalizable} will perform better. For {@link Runnable} and {@link Callable} interfaces
 * GridGain provides analogous {@link IgniteRunnable} and {@link IgniteCallable} classes which are
 * {@link Serializable} and should be used to run computations on the grid.
 * <h1 class="header">Load Balancing</h1>
 * In all cases other than {@code broadcast(...)}, GridGain must select a node for a computation
 * to be executed. The node will be selected based on the underlying {@link LoadBalancingSpi},
 * which by default sequentially picks next available node from grid projection. Other load balancing
 * policies, such as {@code random} or {@code adaptive}, can be configured as well by selecting
 * a different load balancing SPI in grid configuration. If your logic requires some custom
 * load balancing behavior, consider implementing {@link ComputeTask} directly.
 * <h1 class="header">Fault Tolerance</h1>
 * GridGain guarantees that as long as there is at least one grid node standing, every job will be
 * executed. Jobs will automatically failover to another node if a remote node crashed
 * or has rejected execution due to lack of resources. By default, in case of failover, next
 * load balanced node will be picked for job execution. Also jobs will never be re-routed to the
 * nodes they have failed on. This behavior can be changed by configuring any of the existing or a custom
 * {@link FailoverSpi} in grid configuration.
 * <h1 class="header">Resource Injection</h1>
 * All compute jobs, including closures, runnables, callables, and tasks can be injected with
 * grid resources. Both, field and method based injections are supported. The following grid
 * resources can be injected:
 * <ul>
 * <li>{@link IgniteTaskSessionResource}</li>
 * <li>{@link IgniteInstanceResource}</li>
 * <li>{@link IgniteLoggerResource}</li>
 * <li>{@link IgniteHomeResource}</li>
 * <li>{@link IgniteExecutorServiceResource}</li>
 * <li>{@link IgniteLocalNodeIdResource}</li>
 * <li>{@link IgniteMBeanServerResource}</li>
 * <li>{@link IgniteMarshallerResource}</li>
 * <li>{@link IgniteSpringApplicationContextResource}</li>
 * <li>{@link IgniteSpringResource}</li>
 * </ul>
 * Refer to corresponding resource documentation for more information.
 * Here is an example of how to inject instance of {@link Ignite} into a computation:
 * <pre name="code" class="java">
 * public class MyGridJob extends GridRunnable {
 *      ...
 *      &#64;GridInstanceResource
 *      private Grid grid;
 *      ...
 *  }
 * </pre>
 * <h1 class="header">Computation SPIs</h1>
 * Note that regardless of which method is used for executing computations, all relevant SPI implementations
 * configured for this grid instance will be used (i.e. failover, load balancing, collision resolution,
 * checkpoints, etc.). If you need to override configured defaults, you should use compute task together with
 * {@link ComputeTaskSpis} annotation. Refer to {@link ComputeTask} documentation for more information.
 */
public interface IgniteCompute extends IgniteAsyncSupport {
    /**
     * Gets grid projection to which this {@code GridCompute} instance belongs.
     *
     * @return Grid projection to which this {@code GridCompute} instance belongs.
     */
    public ClusterGroup clusterGroup();

    /**
     * Executes given job on the node where data for provided affinity key is located
     * (a.k.a. affinity co-location).
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param cacheName Name of the cache to use for affinity co-location.
     * @param affKey Affinity key.
     * @param job Job which will be co-located on the node with given affinity key.
     * @see ComputeJobContext#cacheName()
     * @see ComputeJobContext#affinityKey()
     * @throws IgniteCheckedException If job failed.
     */
    public void affinityRun(@Nullable String cacheName, Object affKey, Runnable job) throws IgniteCheckedException;

    /**
     * Executes given job on the node where data for provided affinity key is located
     * (a.k.a. affinity co-location).
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param cacheName Name of the cache to use for affinity co-location.
     * @param affKey Affinity key.
     * @param job Job which will be co-located on the node with given affinity key.
     * @return Job result.
     * @throws IgniteCheckedException If job failed.
     * @see ComputeJobContext#cacheName()
     * @see ComputeJobContext#affinityKey()
     */
    public <R> R affinityCall(@Nullable String cacheName, Object affKey, Callable<R> job) throws IgniteCheckedException;

    /**
     * Executes given task on the grid projection. For step-by-step explanation of task execution process
     * refer to {@link ComputeTask} documentation.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param taskCls Class of the task to execute. If class has {@link ComputeTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task name.
     * @param arg Optional argument of task execution, can be {@code null}.
     * @return Task result.
     * @throws IgniteCheckedException If task failed.
     */
    public <T, R> R execute(Class<? extends ComputeTask<T, R>> taskCls, @Nullable T arg) throws IgniteCheckedException;

    /**
     * Executes given task on this grid projection. For step-by-step explanation of task execution process
     * refer to {@link ComputeTask} documentation.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param task Instance of task to execute. If task class has {@link ComputeTaskName} annotation,
     *      then task is deployed under a name specified within annotation. Otherwise, full
     *      class name is used as task name.
     * @param arg Optional argument of task execution, can be {@code null}.
     * @return Task result.
     * @throws IgniteCheckedException If task failed.
     */
    public <T, R> R execute(ComputeTask<T, R> task, @Nullable T arg) throws IgniteCheckedException;

    /**
     * Executes given task on this grid projection. For step-by-step explanation of task execution process
     * refer to {@link ComputeTask} documentation.
     * <p>
     * If task for given name has not been deployed yet, then {@code taskName} will be
     * used as task class name to auto-deploy the task (see {@link #localDeployTask(Class, ClassLoader)} method).
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param taskName Name of the task to execute.
     * @param arg Optional argument of task execution, can be {@code null}.
     * @return Task result.
     * @throws IgniteCheckedException If task failed.
     * @see ComputeTask for information about task execution.
     */
    public <T, R> R execute(String taskName, @Nullable T arg) throws IgniteCheckedException;

    /**
     * Broadcasts given job to all nodes in grid projection.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param job Job to broadcast to all projection nodes.
     * @throws IgniteCheckedException If job failed.
     */
    public void broadcast(Runnable job) throws IgniteCheckedException;

    /**
     * Broadcasts given job to all nodes in grid projection. Every participating node will return a
     * job result. Collection of all returned job results is returned from the result future.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param job Job to broadcast to all projection nodes.
     * @return Collection of results for this execution.
     * @throws IgniteCheckedException If execution failed.
     */
    public <R> Collection<R> broadcast(Callable<R> job) throws IgniteCheckedException;

    /**
     * Broadcasts given closure job with passed in argument to all nodes in grid projection.
     * Every participating node will return a job result. Collection of all returned job results
     * is returned from the result future.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param job Job to broadcast to all projection nodes.
     * @param arg Job closure argument.
     * @return Collection of results for this execution.
     * @throws IgniteCheckedException If execution failed.
     */
    public <R, T> Collection<R> broadcast(IgniteClosure<T, R> job, @Nullable T arg) throws IgniteCheckedException;

    /**
     * Executes provided job on a node in this grid projection.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param job Job closure to execute.
     * @throws IgniteCheckedException If execution failed.
     */
    public void run(Runnable job) throws IgniteCheckedException;

    /**
     * Executes collection of jobs on grid nodes within this grid projection.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param jobs Collection of jobs to execute.
     * @throws IgniteCheckedException If execution failed.
     */
    public void run(Collection<? extends Runnable> jobs) throws IgniteCheckedException;

    /**
     * Executes provided job on a node in this grid projection. The result of the
     * job execution is returned from the result closure.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param job Job to execute.
     * @return Job result.
     * @throws IgniteCheckedException If execution failed.
     */
    public <R> R call(Callable<R> job) throws IgniteCheckedException;

    /**
     * Executes collection of jobs on nodes within this grid projection.
     * Collection of all returned job results is returned from the result future.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param jobs Collection of jobs to execute.
     * @return Collection of job results for this execution.
     * @throws IgniteCheckedException If execution failed.
     */
    public <R> Collection<R> call(Collection<? extends Callable<R>> jobs) throws IgniteCheckedException;

    /**
     * Executes collection of jobs on nodes within this grid projection. The returned
     * job results will be reduced into an individual result by provided reducer.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param jobs Collection of jobs to execute.
     * @param rdc Reducer to reduce all job results into one individual return value.
     * @return Future with reduced job result for this execution.
     * @throws IgniteCheckedException If execution failed.
     */
    public <R1, R2> R2 call(Collection<? extends Callable<R1>> jobs, IgniteReducer<R1, R2> rdc) throws IgniteCheckedException;

    /**
     * Executes provided closure job on a node in this grid projection. This method is different
     * from {@code run(...)} and {@code call(...)} methods in a way that it receives job argument
     * which is then passed into the closure at execution time.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param job Job to run.
     * @param arg Job argument.
     * @return Job result.
     * @throws IgniteCheckedException If execution failed.
     */
    public <R, T> R apply(IgniteClosure<T, R> job, @Nullable T arg) throws IgniteCheckedException;

    /**
     * Executes provided closure job on nodes within this grid projection. A new job is executed for
     * every argument in the passed in collection. The number of actual job executions will be
     * equal to size of the job arguments collection.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param job Job to run.
     * @param args Job arguments.
     * @return Collection of job results.
     * @throws IgniteCheckedException If execution failed.
     */
    public <T, R> Collection<R> apply(IgniteClosure<T, R> job, Collection<? extends T> args) throws IgniteCheckedException;

    /**
     * Executes provided closure job on nodes within this grid projection. A new job is executed for
     * every argument in the passed in collection. The number of actual job executions will be
     * equal to size of the job arguments collection. The returned job results will be reduced
     * into an individual result by provided reducer.
     * <p>
     * Supports asynchronous execution (see {@link IgniteAsyncSupport}).
     *
     * @param job Job to run.
     * @param args Job arguments.
     * @param rdc Reducer to reduce all job results into one individual return value.
     * @return Future with reduced job result for this execution.
     * @throws IgniteCheckedException If execution failed.
     */
    public <R1, R2, T> R2 apply(IgniteClosure<T, R1> job, Collection<? extends T> args,
        IgniteReducer<R1, R2> rdc) throws IgniteCheckedException;

    /**
     * Gets tasks future for active tasks started on local node.
     *
     * @return Map of active tasks keyed by their task task session ID.
     */
    public <R> Map<IgniteUuid, ComputeTaskFuture<R>> activeTaskFutures();

    /**
     * Sets task name for the next executed task on this projection in the <b>current thread</b>.
     * When task starts execution, the name is reset, so one name is used only once. You may use
     * this method to set task name when executing jobs directly, without explicitly
     * defining {@link ComputeTask}.
     * <p>
     * Here is an example.
     * <pre name="code" class="java">
     * GridGain.grid().withName("MyTask").run(new MyRunnable() {...});
     * </pre>
     *
     * @param taskName Task name.
     * @return This {@code GridCompute} instance for chaining calls.
     */
    public IgniteCompute withName(String taskName);

    /**
     * Sets task timeout for the next executed task on this projection in the <b>current thread</b>.
     * When task starts execution, the timeout is reset, so one timeout is used only once. You may use
     * this method to set task name when executing jobs directly, without explicitly
     * defining {@link ComputeTask}.
     * <p>
     * Here is an example.
     * <pre name="code" class="java">
     * GridGain.grid().withTimeout(10000).run(new MyRunnable() {...});
     * </pre>
     *
     * @param timeout Computation timeout in milliseconds.
     * @return This {@code GridCompute} instance for chaining calls.
     */
    public IgniteCompute withTimeout(long timeout);

    /**
     * Sets no-failover flag for the next executed task on this projection in the <b>current thread</b>.
     * If flag is set, job will be never failed over even if remote node crashes or rejects execution.
     * When task starts execution, the no-failover flag is reset, so all other task will use default
     * failover policy, unless this flag is set again.
     * <p>
     * Here is an example.
     * <pre name="code" class="java">
     * GridGain.grid().compute().withNoFailover().run(new MyRunnable() {...});
     * </pre>
     *
     * @return This {@code GridCompute} instance for chaining calls.
     */
    public IgniteCompute withNoFailover();

    /**
     * Explicitly deploys a task with given class loader on the local node. Upon completion of this method,
     * a task can immediately be executed on the grid, considering that all participating
     * remote nodes also have this task deployed.
     * <p>
     * Note that tasks are automatically deployed upon first execution (if peer-class-loading is enabled),
     * so use this method only when the provided class loader is different from the
     * {@code taskClass.getClassLoader()}.
     * <p>
     * Another way of class deployment is deployment from local class path.
     * Classes from local class path always have a priority over P2P deployed ones.
     * <p>
     * Note that class can be deployed multiple times on remote nodes, i.e. re-deployed. GridGain
     * maintains internal version of deployment for each instance of deployment (analogous to
     * class and class loader in Java). Execution happens always on the latest deployed instance.
     * <p>
     * This method has no effect if the class passed in was already deployed.
     *
     * @param taskCls Task class to deploy. If task class has {@link ComputeTaskName} annotation,
     *      then task will be deployed under the name specified within annotation. Otherwise, full
     *      class name will be used as task's name.
     * @param clsLdr Task class loader. This class loader is in charge
     *      of loading all necessary resources for task execution.
     * @throws IgniteCheckedException If task is invalid and cannot be deployed.
     */
    public void localDeployTask(Class<? extends ComputeTask> taskCls, ClassLoader clsLdr) throws IgniteCheckedException;

    /**
     * Gets map of all locally deployed tasks keyed by their task name .
     *
     * @return Map of locally deployed tasks keyed by their task name.
     */
    public Map<String, Class<? extends ComputeTask<?, ?>>> localTasks();

    /**
     * Makes the best attempt to undeploy a task with given name from this grid projection. Note that this
     * method returns immediately and does not wait until the task will actually be
     * undeployed on every node.
     *
     * @param taskName Name of the task to undeploy.
     * @throws IgniteCheckedException Thrown if undeploy failed.
     */
    public void undeployTask(String taskName) throws IgniteCheckedException;

    /** {@inheritDoc} */
    @Override public <R> ComputeTaskFuture<R> future();

    /** {@inheritDoc} */
    @Override public IgniteCompute enableAsync();
}
