FROM postgres:latest

ENV POSTGRES_PASSWORD=postgres
ENV POSTGRES_DB=matching_orders

EXPOSE 5432