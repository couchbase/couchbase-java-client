Couchbase Java Client Testing Guide
===================================

This guide is intended to be used by people who want to work on the internals of the Java SDK and/or run the tests to make sure everything works as expected.

Overview
--------
To prepare your system, make sure to have the 1.6 JDK and Apache Ant (1.8 or up is preferred) installed. Don't try to run the tests directly, as they depend on the Ant configuration inside the `build.xml`. The tests use JUnit as the testing framework.

By default, the tests connect to a cluster on `127.0.0.1` and will use the `default` bucket. This also means that data will be lost as it will be deleted and recreated as the tests go through. Also make sure to have the `Administrator` user with a password of `password` setup.

Also make sure to have a even number of nodes configured to make all tests pass (so, 2 or 4 are preferred). If you just want to give them a shot, one node should also work reasonably well.

Running the Tests
-----------------
Check out the code from gerrit (or github, if you just want to run it) and run `ant test` from the root directory. This will run all tests, expect it to run for about 15 minutes (depending on the performance of your machine and the cluster).

This will generate lots of logs, inside the `build` directory you'll also find the test reports (see the last lines of the `ant test` command for their explicit location).

If you want to make your life easier, run them through a IDE like NetBeans. You can import the project as a `Java Free-Form Project`, which should find your ant-targets automatically. You can then run the `test` target and NetBeans should open the JUnit interface automatically.

Customize the test runs
-----------------------
If you want to change the behavior of the test runs, the `build.xml` is the first place to look at. If you wan to connec to a different IP-Adress, change the following line accordingly:

	<!--test related properties -->
	<property name="server.address_v4" value="127.0.0.1"/>

If you only want to run a single file (thats as atomic as it gets with the current setup), change the following line inside the `test` target:

	<fileset dir="${test.dir}">
	  <include name="**/*Test.java"/>
	</fileset>

For example, to run the `ViewTest.java` individually, use it like this:

	<fileset dir="${test.dir}">
	  <include name="com/couchbase/client/ViewTest.java"/>
	</fileset>

You don't need to change the `ant test` command itself, since it will pick up the modified `build.xml`.

If you want to attach a debugger to the current test session, you need to add those `jvmarg` params down in the `test` target as well:

	<jvmarg value="-Xdebug" />
	<jvmarg value="-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5432" />

The `suspend` param can be changed to let the code wait until the debugger is attached. This is useful if the test runs are very short and you want to inspect instantly.

Writing more tests
------------------
The test files itself are basic JUnit test classes, and adding them insite the `src/test/java/com/couchbase/client` namespace will load them automatically. A simple test case looks like this:

	public class SampleTest {

	  public SampleTest() {
	  }

	  @BeforeClass
	  public static void setUpClass() {
	  }

	  @AfterClass
	  public static void tearDownClass() {
	  }

	  @Before
	  public void setUp() {
	  }

	  @After
	  public void tearDown() {
	  }

	  @Test
	  public void testOf() {
	    assertEquals(true, false);
	  }

	}

Refer to the JUnit documentation for more infos on how to write tests (or look at the myriad of other already written tests for reference).

Finally, currently there is no difference between unit tests and functional/integration tests. If you want to run the tests, have a Couchbase Server 2.0 cluster running.