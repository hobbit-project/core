default:

test:
	docker pull rabbitmq:management
	./start_test_environment.sh &
	mvn clean test
