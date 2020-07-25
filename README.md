# Adapters

### What is this project about?

A springboot based application that extends/completes a self-service BI suite to wrtie to a myriad of destination types. 

Adapters project contains functionality related to providing hooks into communicating with destination(consumers) outside of NextGen environment.

It's main purpose is to poll and process functionality based on events created via Dispatcher and Context Processor.

This project exposes Spring context through available AdapterService interface that will contain the destination-specific communication hooks.
