default:

test:
	docker pull rabbitmq:3-management
	./start_test_environment.sh &
	mvn clean test
