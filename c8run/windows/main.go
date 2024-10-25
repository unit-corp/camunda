package main

import (
	"c8run/internal/unix"
	"c8run/internal/windows"
	"errors"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"time"
)

func printHelp() {
	optionsHelp := `Options:
   --config     - Applies the specified configuration file.
   --detached   - Starts Camunda Run as a detached process
`
	fmt.Print(optionsHelp)

}

func printStatus() {
	endpoints, _ := os.ReadFile("endpoints.txt")
	fmt.Println(string(endpoints))
}

func queryElasticsearchHealth(name string, url string) {
	healthy := false
	for retries := 12; retries >= 0; retries-- {
		fmt.Println("Waiting for " + name + " to start. " + strconv.Itoa(retries) + " retries left")
		time.Sleep(10 * time.Second)
		resp, err := http.Get(url)
		if err != nil {
			continue
		}
		if resp.StatusCode >= 200 && resp.StatusCode <= 400 {
			healthy = true
			break
		}
	}
	if !healthy {
		fmt.Println("Error: " + name + " did not start!")
		os.Exit(1)
	}
	fmt.Println(name + " has successfully been started.")
}

func queryCamundaHealth(c8 C8Run, name string, url string) {
	healthy := false
	for retries := 24; retries >= 0; retries-- {
		fmt.Println("Waiting for " + name + " to start. " + strconv.Itoa(retries) + " retries left")
		time.Sleep(14 * time.Second)
		resp, err := http.Get(url)
		if err != nil {
			continue
		}
		if resp.StatusCode >= 200 && resp.StatusCode <= 400 {
			healthy = true
			break
		}
	}
	if !healthy {
		fmt.Println("Error: " + name + " did not start!")
		os.Exit(1)
	}
	c8.OpenBrowser(name)
	printStatus()
}

func stopProcess(c8 C8Run, pidfile string) {
	if _, err := os.Stat(pidfile); err == nil {
		commandPidText, _ := os.ReadFile(pidfile)
		commandPidStripped := strings.TrimSpace(string(commandPidText))
		commandPid, _ := strconv.Atoi(string(commandPidStripped))

		for _, process := range c8.GetProcessTree(int(commandPid)) {
			process.Kill()
		}
		os.Remove(pidfile)

	} else if errors.Is(err, os.ErrNotExist) {
		// path/to/whatever does *not* exist

	} else {
		// Schrodinger: file may or may not exist. See err for details.

		// Therefore, do *NOT* use !os.IsNotExist(err) to test for file existence

	}

}

func parseCommandLineOptions(args []string, settings *C8RunSettings) *C8RunSettings {
	if len(args) == 0 {
		return settings
	}
	argsToPop := 0
	switch args[0] {
	case "--config":
		if len(args) > 1 {
			argsToPop = 2
			settings.config = args[1]
		} else {
			printHelp()
			os.Exit(1)
		}
	case "--detached":
		settings.detached = true
	default:
		argsToPop = 2
		printHelp()
		os.Exit(1)

	}
	return parseCommandLineOptions(args[argsToPop:], settings)

}

func getC8RunPlatform() C8Run {
	if runtime.GOOS == "windows" {
		return &windows.WindowsC8Run{}
	} else if runtime.GOOS == "linux" || runtime.GOOS == "darwin" {
		return &unix.UnixC8Run{}
	}
	panic("Unsupported operating system")
}

func main() {
	c8 := getC8RunPlatform()
	baseDir, _ := os.Getwd()
	// parentDir, _ := filepath.Dir(baseDir)
	parentDir := baseDir
	// deploymentDir := filepath.Join(parentDir, "configuration", "resources")
	elasticsearchVersion := "8.13.4"
	camundaVersion := "8.6.2"
	if os.Getenv("CAMUNDA_VERSION") != "" {
		camundaVersion = os.Getenv("CAMUNDA_VERSION")
	}
	expectedJavaVersion := 21

	elasticsearchPidPath := filepath.Join(baseDir, "elasticsearch.pid")
	connectorsPidPath := filepath.Join(baseDir, "connectors.pid")
	camundaPidPath := filepath.Join(baseDir, "camunda.pid")

	os.Setenv("ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_CLASSNAME", "io.camunda.zeebe.exporter.ElasticsearchExporter")
	os.Setenv("ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_ARGS_URL", "http://localhost:9200")
	os.Setenv("ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_ARGS_INDEX_PREFIX", "zeebe-record")

	os.Setenv("CAMUNDA_REST_QUERY_ENABLED", "true")
	os.Setenv("CAMUNDA_OPERATE_CSRFPREVENTIONENABLED", "false")
	os.Setenv("CAMUNDA_TASKLIST_CSRFPREVENTIONENABLED", "false")

	// classPath := filepath.Join(parentDir, "configuration", "userlib") + "," + filepath.Join(parentDir, "configuration", "keystore")

	baseCommand := ""
	// insideConfigFlag := false

	if os.Args[1] == "start" {
		baseCommand = "start"
	} else if os.Args[1] == "stop" {
		baseCommand = "stop"
	} else if os.Args[1] == "package" {
		baseCommand = "package"
        } else {
                panic("Unsupported operation")
        }
	fmt.Print("Command: " + baseCommand + "\n")

	var settings C8RunSettings
	if len(os.Args) > 2 {
		parseCommandLineOptions(os.Args[2:], &settings)
	}

	javaHome := os.Getenv("JAVA_HOME")
	javaBinary := "java"
	if javaHome != "" {
		javaBinary = filepath.Join(javaHome, "bin", "java")
	}

	if baseCommand == "start" {
		javaVersion := os.Getenv("JAVA_VERSION")
		if javaVersion == "" {
			javaVersionCmd := c8.GetVersionCmd(javaBinary)
			var out strings.Builder
			var stderr strings.Builder
			javaVersionCmd.Stdout = &out
			javaVersionCmd.Stderr = &stderr
			javaVersionCmd.Run()
			javaVersionOutput := out.String()
			javaVersionOutputSplit := strings.Split(javaVersionOutput, " ")
			if len(javaVersionOutputSplit) < 2 {
				fmt.Println("Java needs to be installed. Please install JDK " + strconv.Itoa(expectedJavaVersion) + " or newer.")
				fmt.Println("If java is already installed, try explicitly setting JAVA_HOME and JAVA_VERSION")
				os.Exit(1)
			}
			output := javaVersionOutputSplit[1]
			os.Setenv("JAVA_VERSION", output)
			javaVersion = output
		}
		fmt.Print("Java version is " + javaVersion + "\n")

		versionSplit := strings.Split(javaVersion, ".")
		if len(versionSplit) == 0 {
			fmt.Println("Java needs to be installed. Please install JDK " + strconv.Itoa(expectedJavaVersion) + " or newer.")
			os.Exit(1)
		}
		javaMajorVersion := versionSplit[0]
		javaMajorVersionInt, _ := strconv.Atoi(javaMajorVersion)
		if javaMajorVersionInt < expectedJavaVersion {
			fmt.Print("You must use at least JDK " + strconv.Itoa(expectedJavaVersion) + " to start Camunda Platform Run.\n")
			os.Exit(1)
		}

		javaOpts := os.Getenv("JAVA_OPTS")
		if javaOpts != "" {
			fmt.Print("JAVA_OPTS: " + javaOpts + "\n")
		}

		os.Setenv("ES_JAVA_OPTS", "-Xms1g -Xmx1g")

		fmt.Print("Starting Elasticsearch " + elasticsearchVersion + "...\n")
		fmt.Print("(Hint: you can find the log output in the 'elasticsearch.log' file in the 'log' folder of your distribution.)\n")

		elasticsearchLogFilePath := filepath.Join(parentDir, "log", "elasticsearch.log")
		elasticsearchLogFile, err := os.OpenFile(elasticsearchLogFilePath, os.O_RDWR|os.O_CREATE, 0644)
		if err != nil {
			fmt.Print("Failed to open file: " + elasticsearchLogFilePath)
			os.Exit(1)
		}

		elasticsearchCmd := c8.GetElasticsearchCmd(elasticsearchVersion, parentDir)

		elasticsearchCmd.Stdout = elasticsearchLogFile
		elasticsearchCmd.Stderr = elasticsearchLogFile
		err = elasticsearchCmd.Start()
		if err != nil {
			panic(err)
		}
		fmt.Print("Process id ", elasticsearchCmd.Process.Pid, "\n")

		elasticsearchPidFile, err := os.OpenFile(elasticsearchPidPath, os.O_RDWR|os.O_CREATE, 0644)
		if err != nil {
			fmt.Print("Failed to open file: " + elasticsearchPidPath)
			os.Exit(1)
		}
		elasticsearchPidFile.Write([]byte(strconv.Itoa(elasticsearchCmd.Process.Pid)))
		queryElasticsearchHealth("Elasticsearch", "http://localhost:9200/_cluster/health?wait_for_status=green&wait_for_active_shards=all&wait_for_no_initializing_shards=true&timeout=120s")

		connectorsCmd := c8.GetConnectorsCmd(javaBinary, parentDir, camundaVersion)
		connectorsLogPath := filepath.Join(parentDir, "log", "connectors.log")
		connectorsLogFile, err := os.OpenFile(connectorsLogPath, os.O_RDWR|os.O_CREATE, 0644)
		if err != nil {
			fmt.Print("Failed to open file: " + connectorsLogPath)
			os.Exit(1)
		}
		connectorsCmd.Stdout = connectorsLogFile
		connectorsCmd.Stderr = connectorsLogFile
		err = connectorsCmd.Start()
		if err != nil {
			panic(err)
		}

		connectorsPidFile, err := os.OpenFile(connectorsPidPath, os.O_RDWR|os.O_CREATE, 0644)
		if err != nil {
			fmt.Print("Failed to open file: " + connectorsPidPath)
			os.Exit(1)
		}
		connectorsPidFile.Write([]byte(strconv.Itoa(connectorsCmd.Process.Pid)))
		var extraArgs string
		if settings.config != "" {
			extraArgs = "--spring.config.location=" + filepath.Join(parentDir, settings.config)
		} else {
			extraArgs = "--spring.config.location=" + filepath.Join(parentDir, "configuration")
		}
		camundaCmd := c8.GetCamundaCmd(camundaVersion, parentDir, extraArgs)
		camundaLogPath := filepath.Join(parentDir, "log", "camunda.log")
		camundaLogFile, err := os.OpenFile(camundaLogPath, os.O_RDWR|os.O_CREATE, 0644)
		if err != nil {
			fmt.Print("Failed to open file: " + camundaLogPath)
			os.Exit(1)
		}
		camundaCmd.Stdout = camundaLogFile
		camundaCmd.Stderr = camundaLogFile
		err = camundaCmd.Start()
		if err != nil {
			panic(err)
		}
		camundaPidFile, err := os.OpenFile(camundaPidPath, os.O_RDWR|os.O_CREATE, 0644)
		if err != nil {
			fmt.Print("Failed to open file: " + camundaPidPath)
			os.Exit(1)
		}
		camundaPidFile.Write([]byte(strconv.Itoa(camundaCmd.Process.Pid)))
		queryCamundaHealth(c8, "Camunda", "http://localhost:8080/operate/login")
	}

	if baseCommand == "stop" {
		stopProcess(c8, elasticsearchPidPath)
		fmt.Println("Elasticsearch is stopped.")
		stopProcess(c8, connectorsPidPath)
		fmt.Println("Connectors is stopped.")
		stopProcess(c8, camundaPidPath)
		fmt.Println("Camunda is stopped.")
	}

	if baseCommand == "package" {
                if runtime.GOOS == "windows" {
		        PackageWindows(camundaVersion, elasticsearchVersion)
                } else if runtime.GOOS == "linux" || runtime.GOOS == "darwin" {
		        PackageUnix(camundaVersion, elasticsearchVersion)
                } else {
                        panic("Unsupported system")
                }
	}

}
